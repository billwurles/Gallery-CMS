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

import static es.burl.cms.TestSites.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
}
