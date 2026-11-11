package es.burl.cms.builder;

import es.burl.cms.data.*;
import es.burl.cms.helper.Filesystem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.*;
import org.thymeleaf.linkbuilder.StandardLinkBuilder;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

@Slf4j
@Component
public class ThymeleafSiteBuilder implements SiteBuilder {

	String baseTemplate = "site/base";
	Path htmlRoot;
	/** Where the build in progress writes: a staging folder, swapped into htmlRoot when it completes. */
	private Path out;
	/** Content hash of the static includes in the build in progress, added to their links. */
	private String assetVersion;

	static final List<String> STATIC_INCLUDES = List.of("style.css", "gallery.js", "error.js");

	/** Longest side of a gallery grid image: grid columns are up to ~460 CSS px, so sharp on 2x screens. */
	static final int THUMB_SIZE = 900;
	/** Longest side of the full-size view's image and the home image. */
	static final int LARGE_SIZE = 2000;

	/** Resized copies of gallery images, cached next to the gallery folder by content hash. */
	private final WebImages webImages;
	Path galleryRoot;
	String staticRoot;
	SpringTemplateEngine engine;
	int postsPerPage;
	String homeUrl;

	@Autowired
	public ThymeleafSiteBuilder(@Qualifier("getHtmlRoot") Path htmlRoot,
								@Qualifier("getGalleryRoot") Path galleryRoot,
								@Qualifier("getStaticRoot") String staticRoot,
								@Qualifier("getPostsPerPage") int postsPerPage,
								@Value("${site.base-path:/}") String basePath,
								SpringTemplateEngine springTemplateEngine){
		this.htmlRoot = htmlRoot;
		this.galleryRoot = galleryRoot;
		this.staticRoot = staticRoot;
		this.postsPerPage = postsPerPage;
		this.engine = springTemplateEngine;
		this.webImages = new WebImages(galleryRoot.toAbsolutePath().normalize().resolveSibling("web-images"));
		this.engine.setLinkBuilder(new ServerlessLinkBuilder(basePath));
		String base = ServerlessLinkBuilder.normalise(basePath);
		this.homeUrl = base.equals("/") ? "/" : base + "/";
		log.info("Publishing with base path {}", ServerlessLinkBuilder.normalise(basePath));
	}

	/**
	 * Builds the whole site into a staging folder next to html_root, then swaps it in. A failed build leaves the
	 * live site as it was, and anything the new build doesn't write (a deleted page, an old image) disappears.
	 * html_root itself is kept (only its contents change): nginx bind-mounts that directory, so replacing it
	 * would leave nginx serving the old one.
	 */
	@Override
	public synchronized void buildSite(Site site) throws IOException {
		log.info("Generating Site {}", site.getName());
		Path staging = htmlRoot.resolveSibling("." + htmlRoot.getFileName() + ".building");
		deleteTree(staging);
		Files.createDirectories(staging);
		out = staging;
		webImages.startBuild();
		try {
			copyStaticIncludes();

			generateErrorPage(site, 404, "Not Found", "The page does not exist on this server, please return <a href=\"" + homeUrl + "\">home</a>");

			generateHomePage(site);
			generatePages(site);

			Path exhibitionsPath = inside(out, site.getExhibitionRepo().getMenuItem().getUrl());
			generateExhibitionsPages(site, exhibitionsPath);
			generateExhibitionsDirectPages(site, exhibitionsPath);
		} catch (IOException | RuntimeException e) {
			log.error("Site build failed, the live site is unchanged", e);
			deleteTree(staging);
			throw e;
		} finally {
			out = null;
		}

		swapIn(staging);
		try {
			webImages.pruneUnused();
		} catch (IOException e) {
			log.warn("Couldn't tidy the resized image cache", e); // the site is published; this is housekeeping
		}
		log.info("Site rendering complete");
	}

	/** Replaces html_root's contents with the staging folder's, by renames on the same filesystem. */
	private void swapIn(Path staging) throws IOException {
		Files.createDirectories(htmlRoot);
		Path old = htmlRoot.resolveSibling("." + htmlRoot.getFileName() + ".old");
		deleteTree(old);
		Files.createDirectories(old);
		for (Path entry : list(htmlRoot)) {
			Files.move(entry, old.resolve(entry.getFileName()));
		}
		for (Path entry : list(staging)) {
			Files.move(entry, htmlRoot.resolve(entry.getFileName()));
		}
		deleteTree(staging);
		deleteTree(old);
	}

	/**
	 * {@code base/name}, refusing a name that leads outside {@code base} ("..", "/", an absolute path).
	 * The editor only accepts plain addresses (see Slugs); this guards against anything older or edited by
	 * hand, and fails the build so the live site stays as it is.
	 */
	static Path inside(Path base, String name) throws IOException {
		Path root = base.toAbsolutePath().normalize();
		Path resolved = root.resolve(name == null ? "" : name).normalize();
		if (name == null || name.isBlank() || !resolved.startsWith(root) || resolved.equals(root)) {
			throw new IOException("Refusing to publish \"" + name + "\": it isn't a plain page or exhibition address");
		}
		return resolved;
	}

	private static List<Path> list(Path dir) throws IOException {
		try (Stream<Path> entries = Files.list(dir)) {
			return entries.toList();
		}
	}

	static void deleteTree(Path dir) throws IOException {
		if (!Files.exists(dir, LinkOption.NOFOLLOW_LINKS)) return;
		try (Stream<Path> paths = Files.walk(dir)) {
			for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
				Files.delete(path);
			}
		}
	}

	private void copyStaticIncludes() throws IOException {
		log.debug("Copying static include files");
		Path includesPath = out.resolve("include");
		Files.createDirectories(includesPath);

		MessageDigest digest = sha256();
		for (String file : STATIC_INCLUDES) {
			Filesystem.copyClassPathResource(file, staticRoot, includesPath);
			digest.update(Files.readAllBytes(includesPath.resolve(file)));
		}
		// Pages link to include/<file>?v=<this>, so a changed stylesheet or script gets a new address and
		// no browser or proxy cache can pair new pages with an old copy
		assetVersion = HexFormat.of().formatHex(digest.digest()).substring(0, 12);
	}

	private static MessageDigest sha256() {
		try {
			return MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	private void generateErrorPage(Site site, int code, String shortText, String message) throws IOException {
		log.debug("Generating error page {}", code);
		Context context = getBaseContext(site.getName(), "error", site.getMenuItems(), "Error "+code, "error");
		context.setVariable("errorCode", code);
		context.setVariable("shortText", shortText);
		context.setVariable("message", message);
		renderTemplateToFile(baseTemplate, context, out, code+".html");
	}

	private void generateHomePage(Site site) throws IOException {
		log.debug("Generating Home page");
		Context context = getBaseContext(site.getName(), "home", site.getMenuItems(), "Home", "home");
		context.setVariable("homeImage", site.getHomeImage());

		String homeImage = site.getHomeImage() == null ? "" : site.getHomeImage().getFilename();
		File file = galleryRoot.resolve(homeImage).toFile();
		if (!homeImage.isBlank() && file.isFile()) {
			log.debug("Publishing home image");
			webImages.publish(file.toPath(), LARGE_SIZE, out.resolve(homeImage));
		} else {
			log.warn("No home image to publish");
		}

		renderTemplate(context, out);
	}

	private void generatePages(Site site) throws IOException {
		for (Page page : site.getPages().values()) {
			log.debug("Generating page {} -hasGallery:{} -content:{}", page.getMenuItem().getUrl(), page.hasGallery(), page.hasContent());
			Path pagePath = inside(out, page.getMenuItem().getUrl());
			Files.createDirectories(pagePath);
			copyPageGallery(page, pagePath);

			log.debug("Generating HTML for page {}", page.getMenuItem().getUrl());
			Context context = getBaseContext(site.getName(), "page", site.getMenuItems(), page.getMenuItem().getTitle(), page.getMenuItem().getUrl());
			context.setVariable("page", page);
			renderTemplate(context, pagePath);
		}
	}

	private void generateExhibitionsPages(Site site, Path exhibitionsPath) throws IOException {
		// At least one page, so the menu's Exhibitions link works before the first exhibition is added
		int totalPages = Math.max(1, site.getExhibitionRepo().getTotalPages(postsPerPage));

		for (int exhibitionPage = 1; exhibitionPage <= totalPages; exhibitionPage++) {
			log.debug("Generating exhibition page {}", exhibitionPage);
			Path exhibitionPagePath = exhibitionsPath;
			if (exhibitionPage > 1) {
				exhibitionPagePath = exhibitionsPath.resolve("page-" + exhibitionPage);
			}
			Files.createDirectories(exhibitionPagePath);
			generateExhibitionsPage(site, exhibitionPage, totalPages, site.getExhibitionRepo().getExhibitionsPage(exhibitionPage, postsPerPage), exhibitionPagePath);
		}
	}

	private void generateExhibitionsPage(Site site, int exhibitionPage, int totalPages, List<Exhibition> exhibitions, Path exhibitionsPath) throws IOException {
		log.debug("Generating Exhibitions page {} of {}",exhibitionPage, totalPages);
		Context context = getBaseContext(site.getName(), "exhibitions", site.getMenuItems(),
				exhibitionPage == 1 ? "Exhibitions" : "Exhibitions - Page "+exhibitionPage, "exhibitions");
		context.setVariable("exhibitionPage", exhibitionPage);
		context.setVariable("totalPages", totalPages);
		context.setVariable("exhibitions", exhibitions);
		renderTemplate(context, exhibitionsPath);
	}

	private void generateExhibitionsDirectPages(Site site, Path exhibitionsPath) throws IOException {
		for (Exhibition exhibition : site.getExhibitionRepo().getExhibitionsInDateOrder()) {
			Path exhibitionPath = inside(exhibitionsPath, exhibition.getUrl());
			Files.createDirectories(exhibitionPath);
			generateFullExhibitionPage(site, exhibition, exhibitionPath);
		}
	}

	private void generateFullExhibitionPage(Site site, Exhibition exhibition, Path exhibitionPath) throws IOException {
		log.debug("Generating exhibition {}",exhibition.getTitle());
		Context context = getBaseContext(site.getName(), "exhibition", site.getMenuItems(), exhibition.getTitle(), "exhibitions");
		context.setVariable("exhibition", exhibition);
		renderTemplate(context, exhibitionPath);
	}

	private void copyPageGallery(Page page, Path pagePath) throws IOException {
		if(page.showsGallery()){ // a hidden gallery's images aren't published
			log.debug("Copying gallery for {}",page.getMenuItem().getUrl());
			// Construct the page path and image directory path, create dirs if they don't exist
			Path imageDir = pagePath.resolve("images");
			Path cmsImageDir = inside(galleryRoot, page.getMenuItem().getUrl());
			Files.createDirectories(imageDir);

			// Each painting gets images/<file> for the full-size view and images/thumbs/<file> for the grid;
			// one whose file is missing is left out of the published gallery folder
			for (Painting painting : page.getGallery().getGallery().values()) {
				Path file = cmsImageDir.resolve(painting.getFilename());
				if (Files.isRegularFile(file)) {
					webImages.publish(file, LARGE_SIZE, imageDir.resolve(painting.getFilename()));
					webImages.publish(file, THUMB_SIZE, imageDir.resolve("thumbs").resolve(painting.getFilename()));
				} else {
					log.warn("Painting {} on page {} has no image file", painting.getFilename(), page.getMenuItem().getUrl());
				}
			}
		}
	}

	private Context getBaseContext(String name, String pageType, List<MenuItem> menuItems, String title, String currentPageUrl){
		Context context = new Context();
		context.setVariable("siteName", name);
		context.setVariable("title", title);
		context.setVariable("pageType", pageType);
		context.setVariable("menuItems", menuItems);
		context.setVariable("currentPage", currentPageUrl);
		context.setVariable("currentYear", java.time.Year.now().getValue());
		context.setVariable("assetVersion", assetVersion);
		return context;
	}

	private void renderTemplate(Context context, Path filePath) throws IOException {
		renderTemplateToFile(baseTemplate, context, filePath, "index.html");
	}

	private void renderTemplateToFile(String templateName, Context context, Path filePath, String filename) throws IOException {
		log.debug("Rendering template: {}", filePath.toString());
		// Render the template to a string
		String renderedContent = engine.process(templateName, context);

		// Save the rendered content to the specified file
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath.resolve(filename).toFile()))) {
			writer.write(renderedContent);
		}
	}
}
