package es.burl.cms.builder;

import es.burl.cms.data.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.*;
import org.thymeleaf.linkbuilder.StandardLinkBuilder;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
	SpringTemplateEngine engine;
	int postsPerPage;

	@Autowired
	public ThymeleafSiteBuilder(@Qualifier("getHtmlRoot") Path htmlRoot,
								@Qualifier("getGalleryRoot") Path galleryRoot,
								@Qualifier("getPostsPerPage") int postsPerPage,
								SpringTemplateEngine springTemplateEngine){
		this.htmlRoot = htmlRoot;
		this.galleryRoot = galleryRoot;
		this.postsPerPage = postsPerPage;
		this.engine = springTemplateEngine;
		this.engine.setLinkBuilder(new serverlessLinkBuilder());
	}

	@Override
	public void buildSite(Site site) throws IOException {
		//TODO: clear the html_root folder, or backup existing and then make new structure

		copyCSS();

		for(Page page : site.getPages().values()){
			log.debug("Generating page {} -inset:{}",page.getMenuItem().getUrl(), page.getInsetImage());
			Path pagePath = htmlRoot.resolve(page.getMenuItem().getUrl());
			if(page.getMenuItem().getUrl().equals("home")) pagePath = htmlRoot;
			Files.createDirectories(pagePath);
			copyPageGallery(page, pagePath);
			generatePageHTML(site, page, pagePath);
		}

		int totalPages = site.getExhibitionRepo().getTotalPages(postsPerPage);
		Path exhibitionPath = htmlRoot.resolve(site.getExhibitionRepo().getExhibitionOrder().getUrl());

		for(int exhibitionPage = 1; exhibitionPage <= totalPages; exhibitionPage++){
			log.debug("Generating exhibition page {}",exhibitionPage);
			Path exhibitionPagePath = exhibitionPath;
			if(exhibitionPage > 1) {
				exhibitionPagePath = exhibitionPath.resolve("page-"+exhibitionPage);
			}
			Files.createDirectories(exhibitionPagePath);
			generateExhibitionsPage(site, exhibitionPage, totalPages, site.getExhibitionRepo().getExhibitionsPage(exhibitionPage, postsPerPage), exhibitionPagePath);
		}

		for(Exhibition exhibition : site.getExhibitionRepo().getExhibitionsInDateOrder()){
			generateFullExhibitionPage(site, exhibition);
		}

	}

	private void copyCSS() throws IOException {
		File templateCSS = new ClassPathResource("templates/site/style.css").getFile();

		Path stylePath = htmlRoot.resolve("css");
		Files.createDirectories(stylePath);
		Files.copy(templateCSS.toPath(), stylePath.resolve("style.css"), StandardCopyOption.REPLACE_EXISTING);
	}

//	private void generateHomepage(Site site) throws IOException {
//		generatePageHTML(site, site.getPage("home"), htmlRoot);
//	}

	private void generatePageHTML(Site site, Page page, Path pagePath) throws IOException {
		Context context = getBaseContext(site.getName(), site.getMenuItems(), page.getMenuItem().getTitle(), page.getMenuItem().getUrl());
		context.setVariable("page", page);
		renderTemplateToFile("site/page", context, pagePath.resolve("index.html"));
	}

	private void generateExhibitionsPage(Site site, int page, int totalPages, List<Exhibition> exhibitions, Path exhibitionPath) throws IOException {
		Context context = getBaseContext(site.getName(), site.getMenuItems(), page == 1 ? "Exhibitions" : "Exhibitions - Page "+page, "exhibitions");
		context.setVariable("page", page);
		context.setVariable("totalPages", totalPages);
		context.setVariable("exhibitions", exhibitions);
		renderTemplateToFile("site/exhibitions", context, exhibitionPath.resolve("index.html"));
	}

	private void generateFullExhibitionPage(Site site, Exhibition exhibition){
		Context context = getBaseContext(site.getName(), site.getMenuItems(), "Exhibition - "+exhibition.getTitle(), "exhibitions");
		context.setVariable("exhibition", exhibition);
	}

	private void copyPageGallery(Page page, Path pagePath) throws IOException {
		// Construct the page path and image directory path, create dirs if they don't exist
		Path imageDir = pagePath.resolve("images");
		File cmsImageDir = galleryRoot.resolve(page.getMenuItem().getUrl()).toFile();
		Files.createDirectories(imageDir);

		//Copy all the images from a gallery
		if(page.hasGallery()){
			for(File file : Objects.requireNonNull(cmsImageDir.listFiles())) {
				log.debug("Found file {}",file.getName());
				Painting painting = page.getGallery().getPainting(file.getName());
				if (painting != null) {
					log.debug("Copying file {}",file.getName());
					Path imagePath = imageDir.resolve(painting.getFilename());
					Files.copy(file.toPath(), imagePath, StandardCopyOption.REPLACE_EXISTING);
				}
			}
		}
	}

	private Context getBaseContext(String name, List<MenuItem> menuItems, String title, String currentPageUrl){
		Context context = new Context();
		context.setVariable("siteName", name);
		context.setVariable("title", title);
		context.setVariable("menuItems", menuItems);
		context.setVariable("currentPage", currentPageUrl);
		context.setVariable("currentYear", java.time.Year.now().getValue());
		return context;
	}

	private void renderTemplateToFile(String templateName, Context context, Path filePath) throws IOException {
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
