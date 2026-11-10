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
import java.util.Comparator;
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
		try {
			copyStaticIncludes();

			generateErrorPage(site, 404, "Not Found", "The page does not exist on this server, please return <a href=\"" + homeUrl + "\">home</a>");

			generateHomePage(site);
			generatePages(site);

			Path exhibitionsPath = out.resolve(site.getExhibitionRepo().getMenuItem().getUrl());
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

		Filesystem.copyClassPathResource("style.css", staticRoot, includesPath);
		Filesystem.copyClassPathResource("gallery.js", staticRoot, includesPath);
		Filesystem.copyClassPathResource("error.js", staticRoot, includesPath);
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
			log.debug("Copying home image");
			Filesystem.copyImage(homeImage, file, out);
		} else {
			log.warn("No home image to publish");
		}

		renderTemplate(context, out);
	}

	private void generatePages(Site site) throws IOException {
		for (Page page : site.getPages().values()) {
			log.debug("Generating page {} -hasGallery:{} -content:{}", page.getMenuItem().getUrl(), page.hasGallery(), page.hasContent());
			Path pagePath = out.resolve(page.getMenuItem().getUrl());
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
			Path exhibitionPath = exhibitionsPath.resolve(exhibition.getUrl());
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
		if(page.hasGallery()){
			log.debug("Copying gallery for {}",page.getMenuItem().getUrl());
			// Construct the page path and image directory path, create dirs if they don't exist
			Path imageDir = pagePath.resolve("images");
			Path cmsImageDir = galleryRoot.resolve(page.getMenuItem().getUrl());
			Files.createDirectories(imageDir);

			// Copy each painting's image; one whose file is missing is left out of the published gallery folder
			for (Painting painting : page.getGallery().getGallery().values()) {
				Path file = cmsImageDir.resolve(painting.getFilename());
				if (Files.isRegularFile(file)) {
					Filesystem.copyImage(painting.getFilename(), file.toFile(), imageDir);
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
