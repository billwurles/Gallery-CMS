package es.burl.cms.builder;

import es.burl.cms.data.Page;
import es.burl.cms.data.Painting;
import es.burl.cms.data.Site;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Year;

import static es.burl.cms.TestSites.*;
import static org.assertj.core.api.Assertions.assertThat;

class ThymeleafSiteBuilderTest {

	@TempDir
	Path tmp;

	Path htmlRoot;
	Path galleryRoot;
	ThymeleafSiteBuilder builder;
	Site site;

	private static final byte[] IMAGE = {1, 2, 3};

	@BeforeEach
	void setUp() throws IOException {
		htmlRoot = Files.createDirectories(tmp.resolve("html"));
		galleryRoot = Files.createDirectories(tmp.resolve("gallery"));

		ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
		resolver.setPrefix("templates/");
		resolver.setSuffix(".html");
		resolver.setTemplateMode(TemplateMode.HTML);
		SpringTemplateEngine engine = new SpringTemplateEngine();
		engine.setTemplateResolver(resolver);

		builder = new ThymeleafSiteBuilder(htmlRoot, galleryRoot, "templates/site/static/", 7, engine);

		site = twoPageSite();
		site.setHomeImage(painting("home.jpg", 0));
		Files.write(galleryRoot.resolve("home.jpg"), IMAGE);
		Path work = Files.createDirectories(galleryRoot.resolve("work"));
		Files.write(work.resolve("a.jpg"), IMAGE);
		Files.write(work.resolve("b.jpg"), IMAGE);
		Files.createDirectories(galleryRoot.resolve("about"));
	}

	private String read(String relative) throws IOException {
		return Files.readString(htmlRoot.resolve(relative));
	}

	private void addExhibitions(int count) {
		for (int i = 0; i < count; i++) {
			site.getExhibitionRepo().add(exhibition("ex-" + i, 1_600_000_000_000L + i * 86_400_000L));
		}
	}

	@Test
	void writesStaticIncludes() throws IOException {
		builder.buildSite(site);

		assertThat(htmlRoot.resolve("include/style.css")).isNotEmptyFile();
		assertThat(htmlRoot.resolve("include/gallery.js")).isNotEmptyFile();
		assertThat(htmlRoot.resolve("include/error.js")).isNotEmptyFile();
	}

	@Test
	void homePageHasTitleMenuAndHomeImage() throws IOException {
		builder.buildSite(site);

		String home = read("index.html");
		assertThat(home).contains("<title>Home - Test Site</title>");
		assertThat(home).contains("About", "Work", "Exhibitions");
		assertThat(home).contains("home.jpg");
		assertThat(htmlRoot.resolve("home.jpg")).hasBinaryContent(IMAGE);
	}

	@Test
	void footerHasSiteNameAndYear() throws IOException {
		builder.buildSite(site);

		assertThat(read("index.html")).contains("Test Site", String.valueOf(Year.now().getValue()));
	}

	@Test
	void notFoundPage() throws IOException {
		builder.buildSite(site);

		assertThat(read("404.html")).contains("Error 404 - Test Site", "Not Found");
	}

	@Test
	void eachPageGetsItsOwnFolderWithContent() throws IOException {
		builder.buildSite(site);

		assertThat(read("about/index.html")).contains("<title>About - Test Site</title>", "<p>About</p>");
		assertThat(read("work/index.html")).contains("<title>Work - Test Site</title>");
	}

	@Test
	void galleryImagesAreCopiedAndListedInOrder() throws IOException {
		builder.buildSite(site);

		assertThat(htmlRoot.resolve("work/images/a.jpg")).hasBinaryContent(IMAGE);
		assertThat(htmlRoot.resolve("work/images/b.jpg")).hasBinaryContent(IMAGE);
		String work = read("work/index.html");
		assertThat(work).contains("images/a.jpg", "images/b.jpg", "Title a.jpg", "10x20cm", "Oil");
		assertThat(work.indexOf("images/a.jpg")).isLessThan(work.indexOf("images/b.jpg"));
	}

	@Test
	void soldPaintingsAreMarked() throws IOException {
		Page work = site.getPage("work");
		work.addPaintingToGallery(painting("a.jpg", 0).toBuilder().sold(true).build());

		builder.buildSite(site);

		assertThat(read("work/index.html")).contains("Sold");
	}

	@Test
	void imagesNotInTheGalleryAreNotPublished() throws IOException {
		Files.write(galleryRoot.resolve("work/orphan.jpg"), IMAGE);

		builder.buildSite(site);

		assertThat(htmlRoot.resolve("work/images/orphan.jpg")).doesNotExist();
	}

	@Test
	void exhibitionsArePaginatedNewestFirst() throws IOException {
		addExhibitions(9); // 7 per page -> 2 pages

		builder.buildSite(site);

		String first = read("exhibitions/index.html");
		String second = read("exhibitions/page-2/index.html");
		assertThat(first).contains("Exhibition ex-8", "Exhibition ex-2").doesNotContain("Exhibition ex-1<");
		assertThat(second).contains("Exhibition ex-1", "Exhibition ex-0");
		assertThat(first.indexOf("Exhibition ex-8")).isLessThan(first.indexOf("Exhibition ex-7"));
		assertThat(second).contains("Exhibitions - Page 2 - Test Site");
		assertThat(htmlRoot.resolve("exhibitions/page-3")).doesNotExist();
	}

	@Test
	void eachExhibitionGetsADetailPage() throws IOException {
		addExhibitions(2);

		builder.buildSite(site);

		assertThat(read("exhibitions/ex-0/index.html")).contains("Exhibition ex-0", "About ex-0");
		assertThat(read("exhibitions/ex-1/index.html")).contains("Exhibition ex-1");
	}

	@Test
	void publishesWithoutAHomeImage() throws IOException {
		site.setHomeImage(Painting.builder().build());

		builder.buildSite(site);

		assertThat(htmlRoot.resolve("index.html")).isNotEmptyFile();
		assertThat(htmlRoot.resolve("about/index.html")).isNotEmptyFile();
	}

	@Test
	void rebuildingOverwritesPreviousOutput() throws IOException {
		builder.buildSite(site);
		site.addNewPage(site.getPage("about").toBuilder().content("<p>Changed</p>").build());

		builder.buildSite(site);

		assertThat(read("about/index.html")).contains("<p>Changed</p>");
	}
}
