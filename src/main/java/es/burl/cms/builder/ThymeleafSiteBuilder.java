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

	Path htmlRoot;
	Path galleryRoot;
	String staticRoot;
	SpringTemplateEngine engine;
	int postsPerPage;
	String homeKey;

	@Autowired
	public ThymeleafSiteBuilder(@Qualifier("getHtmlRoot") Path htmlRoot,
								@Qualifier("getGalleryRoot") Path galleryRoot,
								@Qualifier("getStaticRoot") String staticRoot,
								@Qualifier("getPostsPerPage") int postsPerPage,
								@Qualifier("getHomeKey") String homeKey,
								SpringTemplateEngine springTemplateEngine){
		this.htmlRoot = htmlRoot;
		this.galleryRoot = galleryRoot;
		this.staticRoot = staticRoot;
		this.postsPerPage = postsPerPage;
		this.homeKey = homeKey;
		this.engine = springTemplateEngine;
		this.engine.setLinkBuilder(new serverlessLinkBuilder());
	}

	@Override
	public void buildSite(Site site) throws IOException {
		//TODO: clear the html_root folder, or backup existing and then make new structure
		log.info("Generating Site {}", site.getName());

		copyStaticIncludes();
		generateErrorPage(site, 404, "Not Found", "The page does not exist on this server, please return <a href=\"/\">home</a>");

		for (Page page : site.getPages().values()) {
			log.debug("Generating page {} -hasGallery:{} -content:{}", page.getMenuItem().getUrl(), page.hasGallery(), page.hasContent());
			Path pagePath = htmlRoot.resolve(page.getMenuItem().getUrl());

			if (page.getMenuItem().getUrl().equals(homeKey)) {
				pagePath = htmlRoot; //Home goes to the root, don't use it's funky url
			}
			Files.createDirectories(pagePath);
			copyPageGallery(page, pagePath);
			copyPageInset(page, pagePath);
			generatePageHTML(site, page, pagePath);
		}

		int totalPages = site.getExhibitionRepo().getTotalPages(postsPerPage);
		Path exhibitionsPath = htmlRoot.resolve(site.getExhibitionRepo().getMenuItem().getUrl());

		for (int exhibitionPage = 1; exhibitionPage <= totalPages; exhibitionPage++) {
			log.debug("Generating exhibition page {}", exhibitionPage);
			Path exhibitionPagePath = exhibitionsPath;
			if (exhibitionPage > 1) {
				exhibitionPagePath = exhibitionsPath.resolve("page-" + exhibitionPage);
			}
			Files.createDirectories(exhibitionPagePath);
			generateExhibitionsPage(site, exhibitionPage, totalPages, site.getExhibitionRepo().getExhibitionsPage(exhibitionPage, postsPerPage), exhibitionPagePath);
		}

		for (Exhibition exhibition : site.getExhibitionRepo().getExhibitionsInDateOrder()) {
			Path exhibitionPath = exhibitionsPath.resolve(exhibition.getUrl());
			Files.createDirectories(exhibitionPath);
			generateFullExhibitionPage(site, exhibition, exhibitionPath);
		}

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
		renderTemplateToFile("site/base", context, htmlRoot.resolve(code+".html"));
	}


	private void generatePageHTML(Site site, Page page, Path pagePath) throws IOException {
		log.debug("Generating HTML for page {}", page.getMenuItem().getUrl());
		Context context = getBaseContext(site.getName(), "page", site.getMenuItems(), page.getMenuItem().getTitle(), page.getMenuItem().getUrl());
		context.setVariable("page", page);
		renderTemplateToFile("site/base", context, pagePath.resolve("index.html"));
	}

	private void generateExhibitionsPage(Site site, int exhibitionPage, int totalPages, List<Exhibition> exhibitions, Path exhibitionsPath) throws IOException {
		log.debug("Generating Exhibitions page {} of {}",exhibitionPage, totalPages);
		Context context = getBaseContext(site.getName(), "exhibitions", site.getMenuItems(),
				exhibitionPage == 1 ? "Exhibitions" : "Exhibitions - Page "+exhibitionPage, "exhibitions");
		context.setVariable("exhibitionPage", exhibitionPage);
		context.setVariable("totalPages", totalPages);
		context.setVariable("exhibitions", exhibitions);
		renderTemplateToFile("site/base", context, exhibitionsPath.resolve("index.html"));
	}

	private void generateFullExhibitionPage(Site site, Exhibition exhibition, Path exhibitionPath) throws IOException {
		log.debug("Genearating exhibition {}",exhibition.getTitle());
		Context context = getBaseContext(site.getName(), "exhibition", site.getMenuItems(), exhibition.getTitle(), "exhibitions");
		context.setVariable("exhibition", exhibition);
		renderTemplateToFile("site/base", context, exhibitionPath.resolve("index.html"));
	}

	private void copyPageInset(Page page, Path pagePath) throws IOException {
		if(page.hasInsetImage()){
			log.debug("Copying inset image for {}",page.getMenuItem().getUrl());
			Path imageDir = pagePath.resolve("images").resolve("inset");
			Path cmsImageDir = galleryRoot.resolve(page.getMenuItem().getUrl()).resolve("images").resolve("inset");
			Files.createDirectories(imageDir);

			File file = cmsImageDir.resolve(page.getInsetImage().getFilename()).toFile();
			Filesystem.copyImage(page.getInsetImage().getFilename(), file, imageDir);
		}
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

	private void renderTemplateToFile(String templateName, Context context, Path filePath) throws IOException {
		log.debug("Rendering template: {}", filePath.toString());
		// Render the template to a string
		String renderedContent = engine.process(templateName, context);

		// Save the rendered content to the specified file
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath.toFile()))) {
			writer.write(renderedContent);
		}
	}

	private class serverlessLinkBuilder extends StandardLinkBuilder {

		@Override
		protected String computeContextPath(final IExpressionContext context,
											final String base,
											final Map<String, Object> parameters) {

			if (context instanceof IWebContext) {
				return super.computeContextPath(context, base, parameters);
			}

			return "/"; //assuming css and images are here

		}
	}
}
