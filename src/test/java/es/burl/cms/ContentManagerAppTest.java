package es.burl.cms;

import com.fasterxml.jackson.databind.ObjectMapper;
import es.burl.cms.data.Site;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static es.burl.cms.TestSites.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Starts the whole app against a temporary appdata folder seeded with a backup,
 * renders every editor page through the real templates, and publishes the site.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ContentManagerAppTest {

	static final Path ROOT;
	static final Path GALLERY;
	static final Path HTML;
	static final Path BACKUP;

	static {
		try {
			ROOT = Files.createTempDirectory("cms-app-test");
			GALLERY = Files.createDirectories(ROOT.resolve("gallery"));
			HTML = Files.createDirectories(ROOT.resolve("html"));
			BACKUP = Files.createDirectories(ROOT.resolve("backup"));
			seed();
		} catch (IOException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private static void seed() throws IOException {
		Site site = twoPageSite();
		site.setHomeImage(painting("home.jpg", 0));
		site.getExhibitionRepo().add(exhibition("spring-show", 1_700_000_000_000L));
		Files.write(GALLERY.resolve("home.jpg"), new byte[]{1});
		Path work = Files.createDirectories(GALLERY.resolve("work"));
		Files.write(work.resolve("a.jpg"), new byte[]{1});
		Files.write(work.resolve("b.jpg"), new byte[]{1});
		Files.writeString(BACKUP.resolve("1-unpublished-12000020260101-site-data-backup.json"),
				new ObjectMapper().writeValueAsString(site));
	}

	@DynamicPropertySource
	static void paths(DynamicPropertyRegistry registry) {
		registry.add("gallery.root", GALLERY::toString);
		registry.add("html.root", HTML::toString);
		registry.add("backup.path", BACKUP::toString);
	}

	@Autowired
	MockMvc mvc;

	@Autowired
	@Qualifier("getSite")
	Site site;

	@Test
	void restoresTheSiteFromTheBackupAtStartup() {
		assertThat(site.getName()).isEqualTo("Test Site");
		assertThat(site.getPages()).containsOnlyKeys("about", "work");
		assertThat(site.getExhibitionRepo().get("spring-show")).isNotNull();
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"/",
			"/upload",
			"/page",
			"/page/about",
			"/page/work/gallery",
			"/page/work/gallery/upload",
			"/exhibitions",
			"/exhibitions/new",
			"/exhibitions/spring-show",
	})
	void editorPagesRender(String url) throws Exception {
		String html = mvc.perform(get(url))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		assertThat(html).contains("</html>").contains("About", "Work");
	}

	@ParameterizedTest
	@ValueSource(strings = {"/", "/page", "/page/about", "/page/work/gallery", "/exhibitions", "/exhibitions/spring-show"})
	void editorLinksCarryTheProxyPrefix(String url) throws Exception {
		String html = mvc.perform(get(url).header("X-Forwarded-Prefix", "/gallery-cms-editor"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		Matcher m = Pattern.compile("(?:href|src|action|data-url)=\"(/[^\"]*)\"").matcher(html);
		List<String> links = new ArrayList<>();
		while (m.find()) links.add(m.group(1));
		assertThat(links).isNotEmpty().allMatch(l -> l.startsWith("/gallery-cms-editor/"), "starts with the prefix");
	}

	@Test
	void editorScriptUrlsCarryTheProxyPrefix() throws Exception {
		String home = mvc.perform(get("/").header("X-Forwarded-Prefix", "/gallery-cms-editor"))
				.andReturn().getResponse().getContentAsString();

		// Thymeleaf escapes "/" as "\/" inside inlined JavaScript strings
		assertThat(home).contains("\"\\/gallery-cms-editor\\/rearrange\"").doesNotContain("'/rearrange'");
	}

	@Test
	void redirectsCarryTheProxyPrefix() throws Exception {
		mvc.perform(post("/page/about")
						.header("X-Forwarded-Prefix", "/gallery-cms-editor")
						.header("X-Forwarded-Proto", "https")
						.header("X-Forwarded-Host", "dev.burl.es")
						.param("title", "About").param("url", "about").param("content", "<p>About</p>"))
				.andExpect(redirectedUrl("https://dev.burl.es/gallery-cms-editor/page/about"));
	}

	@Test
	void publishWritesTheStaticSite() throws Exception {
		mvc.perform(get("/save")).andExpect(status().isOk());

		assertThat(HTML.resolve("index.html")).isNotEmptyFile();
		assertThat(HTML.resolve("about/index.html")).isNotEmptyFile();
		assertThat(HTML.resolve("work/images/a.jpg")).exists();
		assertThat(HTML.resolve("exhibitions/spring-show/index.html")).isNotEmptyFile();
		try (var files = Files.list(BACKUP)) {
			assertThat(files.map(p -> p.getFileName().toString())).anyMatch(n -> n.startsWith("1-published-"));
		}
	}

	@Test
	void unknownPagesGetTheEditorsNotFoundPage() throws Exception {
		String body = mvc.perform(get("/page/no-such-page"))
				.andExpect(status().isNotFound())
				.andReturn().getResponse().getContentAsString();

		assertThat(body).contains("The page no-such-page was not found");
	}

	@org.junit.jupiter.params.ParameterizedTest
	@org.junit.jupiter.params.provider.ValueSource(strings = {"/", "/page", "/page/about", "/page/work/gallery",
			"/page/work/gallery/upload", "/upload", "/exhibitions", "/exhibitions/new", "/exhibitions/spring-show"})
	void editorPagesUseTheEditorStylesheetOnly(String path) throws Exception {
		String body = mvc.perform(get(path))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		assertThat(body)
				.containsPattern("/css/editor-[0-9a-f]{32}\\.css").contains("class=\"sidebar\"", "Test Site")
				.doesNotContain("bootstrap", "jquery-ui.min.css", "/css/style.css");
	}

	@Test
	void aNewExhibitionSavesToARealAddress() throws Exception {
		String form = mvc.perform(get("/exhibitions/new")).andReturn().getResponse().getContentAsString();

		assertThat(form).contains("action=\"/exhibitions/new/save\"");
	}

	@Test
	void editorStylesheetAndScriptLinksCarryAContentHash() throws Exception {
		String home = mvc.perform(get("/").header("X-Forwarded-Prefix", "/gallery-cms-editor"))
				.andReturn().getResponse().getContentAsString();

		java.util.regex.Matcher css = java.util.regex.Pattern.compile("href=\"/gallery-cms-editor(/css/editor-[0-9a-f]{32}\\.css)\"").matcher(home);
		java.util.regex.Matcher js = java.util.regex.Pattern.compile("src=\"/gallery-cms-editor(/js/scripts-[0-9a-f]{32}\\.js)\"").matcher(home);
		assertThat(css.find()).as("versioned stylesheet link in %s", home).isTrue();
		assertThat(js.find()).as("versioned script link").isTrue();

		var response = mvc.perform(get(js.group(1))).andExpect(status().isOk()).andReturn().getResponse();
		assertThat(response.getContentAsString()).contains("function confirmSave");
		assertThat(response.getHeader("Cache-Control")).contains("no-cache");
		mvc.perform(get(css.group(1))).andExpect(status().isOk());
	}

	@org.junit.jupiter.params.ParameterizedTest
	@org.junit.jupiter.params.provider.ValueSource(strings = {"/page/about", "/exhibitions/spring-show"})
	void theHiddenContentFieldIsOutsideTheRichTextBox(String path) throws Exception {
		String body = mvc.perform(get(path)).andReturn().getResponse().getContentAsString();

		// Inside the box, the editor would read the field as part of the content and save it
		assertThat(body).contains("<div id=\"richTextEditor\"></div>").contains("<textarea");
	}
}
