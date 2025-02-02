package es.burl.cms.builder;

import es.burl.cms.data.*;
import es.burl.cms.helper.Filesystem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.*;
import org.thymeleaf.linkbuilder.StandardLinkBuilder;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
public class ThymeleafSiteBuilder implements SiteBuilder {

	String baseTemplate = "site/base";
	Path htmlRoot;
	Path galleryRoot;
	String staticRoot;
	SpringTemplateEngine engine;
	int postsPerPage;

	@Autowired
	public ThymeleafSiteBuilder(@Qualifier("getHtmlRoot") Path htmlRoot,
								@Qualifier("getGalleryRoot") Path galleryRoot,
								@Qualifier("getStaticRoot") String staticRoot,
								@Qualifier("getPostsPerPage") int postsPerPage,
								SpringTemplateEngine springTemplateEngine){
		this.htmlRoot = htmlRoot;
		this.galleryRoot = galleryRoot;
		this.staticRoot = staticRoot;
		this.postsPerPage = postsPerPage;
		this.engine = springTemplateEngine;
		this.engine.setLinkBuilder(new ServerlessLinkBuilder());
	}

	@Override
	public void buildSite(Site site) throws IOException {
		//TODO: clear the html_root folder, or backup existing and then make new structure
		log.info("Generating Site {}", site.getName());

		copyStaticIncludes();

		generateErrorPage(site, 404, "Not Found", "The page does not exist on this server, please return <a href=\"/\">home</a>");

		generateHomePage(site);
		generatePages(site);

		Path exhibitionsPath = htmlRoot.resolve(site.getExhibitionRepo().getMenuItem().getUrl());
		generateExhibitionsPages(site, exhibitionsPath);
		generateExhibitionsDirectPages(site, exhibitionsPath);

		log.info("Site rendering complete");
	}

	private void copyStaticIncludes() throws IOException {
		log.debug("Copying static include files");
		Path includesPath = htmlRoot.resolve("include");
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
		renderTemplateToFile(baseTemplate, context, htmlRoot, code+".html");
	}

	private void generateHomePage(Site site) throws IOException {
		log.debug("Generating Home page");
		Context context = getBaseContext(site.getName(), "home", site.getMenuItems(), "Home", "home");
		context.setVariable("homeImage", site.getHomeImage());

		log.debug("Copying home image");
		Files.createDirectories(galleryRoot);

		File file = galleryRoot.resolve(site.getHomeImage().getFilename()).toFile();
		Filesystem.copyImage(site.getHomeImage().getFilename(), file, htmlRoot);

		renderTemplate(context, htmlRoot);
	}

	private void generatePages(Site site) throws IOException {
		for (Page page : site.getPages().values()) {
			log.debug("Generating page {} -hasGallery:{} -content:{}", page.getMenuItem().getUrl(), page.hasGallery(), page.hasContent());
			Path pagePath = htmlRoot.resolve(page.getMenuItem().getUrl());
			Files.createDirectories(pagePath);
			copyPageGallery(page, pagePath);

			log.debug("Generating HTML for page {}", page.getMenuItem().getUrl());
			Context context = getBaseContext(site.getName(), "page", site.getMenuItems(), page.getMenuItem().getTitle(), page.getMenuItem().getUrl());
			context.setVariable("page", page);
			renderTemplate(context, pagePath);
		}
	}

	private void generateExhibitionsPages(Site site, Path exhibitionsPath) throws IOException {
		int totalPages = site.getExhibitionRepo().getTotalPages(postsPerPage);

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
			File cmsImageDir = galleryRoot.resolve(page.getMenuItem().getUrl()).toFile();
			Files.createDirectories(imageDir);

			//Copy all the images from a gallery
			for(File file : Objects.requireNonNull(cmsImageDir.listFiles())) {
//				log.debug("Found file {}",file.getName());
				Painting painting = page.getGallery().getPainting(file.getName());
				if(painting != null) {
					log.debug("Copying file: {} from painting {}", file, painting);
					Filesystem.copyImage(painting.getFilename(), file, imageDir);
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
