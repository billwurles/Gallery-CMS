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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static es.burl.cms.TestSites.*;
import static org.assertj.core.api.Assertions.assertThat;

class ThymeleafSiteBuilderTest {

	@TempDir
	Path tmp;

	Path htmlRoot;
	Path galleryRoot;
	ThymeleafSiteBuilder builder;
	SpringTemplateEngine engine;
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
		engine = new SpringTemplateEngine();
		engine.setTemplateResolver(resolver);

		builder = builderWithBasePath("/");

		site = twoPageSite();
		site.setHomeImage(painting("home.jpg", 0));
		Files.write(galleryRoot.resolve("home.jpg"), IMAGE);
		Path work = Files.createDirectories(galleryRoot.resolve("work"));
		Files.write(work.resolve("a.jpg"), IMAGE);
		Files.write(work.resolve("b.jpg"), IMAGE);
		Files.createDirectories(galleryRoot.resolve("about"));
	}

	private ThymeleafSiteBuilder builderWithBasePath(String basePath) {
		return new ThymeleafSiteBuilder(htmlRoot, galleryRoot, "templates/site/static/", 7, basePath, engine);
	}

	/** Every root-absolute href/src in the published HTML. */
	private List<String> rootLinks() throws IOException {
		Pattern link = Pattern.compile("(?:href|src)=\"(/[^\"]*)\"");
		List<String> links = new ArrayList<>();
		try (Stream<Path> files = Files.walk(htmlRoot)) {
			for (Path file : files.filter(f -> f.toString().endsWith(".html")).toList()) {
				Matcher m = link.matcher(Files.readString(file));
				while (m.find()) links.add(m.group(1));
			}
		}
		return links;
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
	void linksAreRootAbsoluteByDefault() throws IOException {
		addExhibitions(9);

		builder.buildSite(site);

		String home = read("index.html");
		assertThat(home).containsPattern("href=\"/include/style\\.css\\?v=[0-9a-f]{12}\"").contains("href=\"/about/\"", "href=\"/exhibitions/\"");
		assertThat(read("exhibitions/index.html")).contains("href=\"/exhibitions/page-2/\"", "href=\"/exhibitions/ex-8/\"");
	}

	@Test
	void linksCarryTheBasePathWhenServedUnderAPrefix() throws IOException {
		addExhibitions(9);
		builder = builderWithBasePath("/gallery-cms/");

		builder.buildSite(site);

		String home = read("index.html");
		assertThat(home).containsPattern("href=\"/gallery-cms/include/style\\.css\\?v=[0-9a-f]{12}\"").contains("href=\"/gallery-cms/about/\"",
				"href=\"/gallery-cms/exhibitions/\"");
		assertThat(read("about/index.html")).contains("href=\"/gallery-cms/\"");
		String exhibitions = read("exhibitions/page-2/index.html");
		assertThat(exhibitions).contains("href=\"/gallery-cms/exhibitions/\"", "href=\"/gallery-cms/exhibitions/ex-1/\"");
		assertThat(read("work/index.html")).containsPattern("src=\"/gallery-cms/include/gallery\\.js\\?v=[0-9a-f]{12}\"").contains("src=\"images/a.jpg\"");
		assertThat(rootLinks()).isNotEmpty().allMatch(l -> l.startsWith("/gallery-cms/"));
	}

	@Test
	void rebuildingOverwritesPreviousOutput() throws IOException {
		builder.buildSite(site);
		site.addNewPage(site.getPage("about").toBuilder().content("<p>Changed</p>").build());

		builder.buildSite(site);

		assertThat(read("about/index.html")).contains("<p>Changed</p>");
	}

	@Test
	void aDeletedPageIsGoneAfterTheNextPublish() throws IOException {
		builder.buildSite(site);
		assertThat(htmlRoot.resolve("about/index.html")).exists();

		site.removePage("about");
		builder.buildSite(site);

		assertThat(htmlRoot.resolve("about")).doesNotExist();
		assertThat(htmlRoot.resolve("404.html")).exists();
		assertThat(htmlRoot.resolve("work/index.html")).exists();
	}

	@Test
	void aFailedBuildLeavesThePreviousSiteInPlace() throws IOException {
		builder.buildSite(site);
		String before = read("about/index.html");

		// A page whose folder would clash with the 404 page's file makes the build fail part way
		site.addNewPage(page("Clash", "404.html", 5));
		site.addNewPage(site.getPage("about").toBuilder().content("<p>Changed</p>").build());

		org.assertj.core.api.Assertions.assertThatThrownBy(() -> builder.buildSite(site)).isInstanceOf(IOException.class);

		assertThat(read("about/index.html")).isEqualTo(before);
		assertThat(htmlRoot.resolve("work/images/a.jpg")).exists();
		try (Stream<Path> siblings = Files.list(tmp)) {
			assertThat(siblings.map(p -> p.getFileName().toString())).containsExactlyInAnyOrder("html", "gallery");
		}
	}

	@Test
	void theHtmlRootFolderItselfIsKept() throws IOException {
		// nginx bind-mounts html_root, so publishing must change its contents and not replace the folder
		Object before = Files.readAttributes(htmlRoot, java.nio.file.attribute.BasicFileAttributes.class).fileKey();
		builder.buildSite(site);
		builder.buildSite(site);

		assertThat(Files.readAttributes(htmlRoot, java.nio.file.attribute.BasicFileAttributes.class).fileKey()).isEqualTo(before);
	}

	@Test
	void aGalleryWithoutAnImageFolderStillPublishes() throws IOException {
		Page empty = page("Empty", "empty", 4);
		empty.addPaintingToGallery(painting("gone.jpg", 0));
		site.addNewPage(empty);

		builder.buildSite(site);

		assertThat(htmlRoot.resolve("empty/index.html")).exists();
		assertThat(htmlRoot.resolve("empty/images/gone.jpg")).doesNotExist();
	}

	@Test
	void theExhibitionsPageExistsWithNoExhibitions() throws IOException {
		builder.buildSite(site);

		assertThat(htmlRoot.resolve("exhibitions/index.html")).exists();
		assertThat(htmlRoot.resolve("exhibitions/page-2")).doesNotExist();
	}

	@Test
	void stylesheetAndScriptLinksCarryTheirContentHash() throws Exception {
		builder.buildSite(site);

		// The version is the hash of the published includes, so it changes whenever any of them does
		java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
		for (String file : ThymeleafSiteBuilder.STATIC_INCLUDES) digest.update(Files.readAllBytes(htmlRoot.resolve("include/" + file)));
		String version = java.util.HexFormat.of().formatHex(digest.digest()).substring(0, 12);

		assertThat(read("index.html")).contains("href=\"/include/style.css?v=" + version + "\"");
		assertThat(read("work/index.html")).contains("src=\"/include/gallery.js?v=" + version + "\"");
		assertThat(read("404.html")).contains("src=\"/include/error.js?v=" + version + "\"");
	}

	@Test
	void blankTextLeavesAPureGallery() throws IOException {
		site.addNewPage(site.getPage("work").toBuilder().content("<p><br></p>").build());

		builder.buildSite(site);

		assertThat(read("work/index.html")).doesNotContain("content-text").contains("painting-open");
	}

	@Test
	void aHiddenGalleryIsNotPublished() throws IOException {
		site.addNewPage(site.getPage("work").toBuilder().content("<p>Words</p>").showGallery(false).build());

		builder.buildSite(site);

		assertThat(read("work/index.html")).contains("<p>Words</p>").doesNotContain("painting-open", "gallery.js");
		assertThat(htmlRoot.resolve("work/images")).doesNotExist();
	}

	@Test
	void hiddenTextIsNotPublished() throws IOException {
		site.addNewPage(site.getPage("work").toBuilder().content("<p>Words</p>").showContent(false).build());

		builder.buildSite(site);

		assertThat(read("work/index.html")).doesNotContain("Words", "content-text").contains("painting-open");
	}

	@Test
	void oneExhibitionPageHasNoPagination() throws IOException {
		addExhibitions(3);

		builder.buildSite(site);

		assertThat(read("exhibitions/index.html")).doesNotContain("class=\"pagination\"", "page-2");
	}

	@Test
	void paginationOnlyLinksToPagesThatExist() throws IOException {
		addExhibitions(9); // two pages of 7

		builder.buildSite(site);

		String first = read("exhibitions/index.html"), second = read("exhibitions/page-2/index.html");
		assertThat(first).contains("Next »").doesNotContain("href=\"/exhibitions/page-3/\"");
		assertThat(second).doesNotContain("page-3");
		for (String link : rootLinks()) {
			if (!link.startsWith("/exhibitions/")) continue;
			assertThat(htmlRoot.resolve(link.substring(1)).resolve("index.html")).as(link).exists();
		}
	}

	@Test
	void theGridGetsSmallCopiesAndTheFullSizeViewLargeOnes() throws IOException {
		WebImagesTest.jpeg(galleryRoot.resolve("work/a.jpg"), 4000, 2000, 0);   // big upload
		WebImagesTest.jpeg(galleryRoot.resolve("work/b.jpg"), 800, 600, 0);     // already small
		WebImagesTest.jpeg(galleryRoot.resolve("home.jpg"), 3200, 1600, 0);

		builder.buildSite(site);

		assertThat(WebImagesTest.size(htmlRoot.resolve("work/images/thumbs/a.jpg"))).containsExactly(900, 450);
		assertThat(WebImagesTest.size(htmlRoot.resolve("work/images/a.jpg"))).containsExactly(2000, 1000);
		assertThat(WebImagesTest.size(htmlRoot.resolve("work/images/thumbs/b.jpg"))).containsExactly(800, 600);
		assertThat(htmlRoot.resolve("work/images/b.jpg")).hasSameBinaryContentAs(galleryRoot.resolve("work/b.jpg"));
		assertThat(WebImagesTest.size(htmlRoot.resolve("home.jpg"))).containsExactly(2000, 1000);

		String work = read("work/index.html");
		assertThat(work).contains("src=\"images/thumbs/a.jpg\"", "data-src=\"images/a.jpg\"");
	}

	@Test
	void aPageAddressLeadingOutsideTheSiteFailsThePublish() throws IOException {
		builder.buildSite(site);
		String before = read("about/index.html");
		// can't be made in the editor any more, but an old or hand-edited backup could hold one
		site.addNewPage(page("Escape", "../escaped", 9));

		org.assertj.core.api.Assertions.assertThatThrownBy(() -> builder.buildSite(site))
				.isInstanceOf(IOException.class).hasMessageContaining("../escaped");

		assertThat(tmp.resolve("escaped")).doesNotExist();
		assertThat(read("about/index.html")).isEqualTo(before);
	}
}
