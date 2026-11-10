package es.burl.cms.controllers.editor;

import es.burl.cms.backup.BackupSite;
import es.burl.cms.data.Painting;
import es.burl.cms.data.Site;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static es.burl.cms.TestSites.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GalleryControllerTest {

	@TempDir
	Path galleryRoot;

	Site site;
	BackupSite backup;
	MockMvc mvc;

	@BeforeEach
	void setUp() {
		site = twoPageSite();
		backup = mock(BackupSite.class);
		mvc = MockMvcBuilders.standaloneSetup(new GalleryController(site, galleryRoot, backup)).build();
	}

	@Test
	void editGallery() throws Exception {
		mvc.perform(get("/page/work/gallery"))
				.andExpect(status().isOk())
				.andExpect(view().name("editor/EditGallery"))
				.andExpect(model().attribute("page", site.getPage("work")))
				.andExpect(model().attribute("message", "Editing gallery Work"))
				.andExpect(model().attribute("hasGallery", true));
	}

	@Test
	void saveGalleryReplacesMetadata() throws Exception {
		String json = """
				{"gallery":{
				  "a.jpg":{"title":"Renamed","filename":"a.jpg","dimensions":"1x1","medium":"Ink","sold":true,"order":1},
				  "b.jpg":{"title":"B","filename":"b.jpg","dimensions":"","medium":"","sold":false,"order":0}
				}}""";

		mvc.perform(post("/page/work/gallery").contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("success"));

		Painting a = site.getPage("work").getGallery().getPainting("a.jpg");
		assertThat(a.getTitle()).isEqualTo("Renamed");
		assertThat(a.isSold()).isTrue();
		assertThat(site.getPage("work").getGallery().getGalleryInOrder())
				.extracting(Painting::getFilename).containsExactly("b.jpg", "a.jpg");
		verify(backup).backup(site);
	}

	@Test
	void uploadPage() throws Exception {
		mvc.perform(get("/page/work/gallery/upload"))
				.andExpect(view().name("editor/UploadGallery"))
				.andExpect(model().attribute("page", "work"));
	}

	@Test
	void uploadPageForUnknownPageIs404View() throws Exception {
		mvc.perform(get("/page/missing/gallery/upload")).andExpect(view().name("404"));
	}

	@Test
	void uploadAddsPaintingsAndWritesFiles() throws Exception {
		String data = Base64.getEncoder().encodeToString(new byte[]{7, 7, 7});
		String json = """
				{"images":[
				  {"title":"New one","filename":"x.jpg","dimensions":"","medium":"","sold":false,"order":0,"imageData":"%s"},
				  {"title":"New two","filename":"y.png","dimensions":"","medium":"","sold":false,"order":0,"imageData":"%s"}
				]}""".formatted(data, data);

		mvc.perform(post("/page/work/gallery/upload").contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isOk());

		var gallery = site.getPage("work").getGallery();
		assertThat(gallery.getGallery()).hasSize(4);
		assertThat(gallery.getGalleryInOrder()).extracting(Painting::getTitle).endsWith("New one", "New two");
		for (Painting p : gallery.getGallery().values()) {
			if (p.getTitle().startsWith("New")) {
				assertThat(galleryRoot.resolve("work").resolve(p.getFilename())).hasBinaryContent(new byte[]{7, 7, 7});
			}
		}
		verify(backup).backup(site);
	}

	@Test
	void uploadToUnknownPageIs404() throws Exception {
		mvc.perform(post("/page/missing/gallery/upload").contentType(MediaType.APPLICATION_JSON).content("{\"images\":[]}"))
				.andExpect(status().isNotFound());

		verifyNoInteractions(backup);
	}

	@Test
	void servesAnImage() throws Exception {
		Files.write(Files.createDirectories(galleryRoot.resolve("work")).resolve("a.jpg"), new byte[]{4, 2});

		mvc.perform(get("/page/work/gallery/image/a.jpg"))
				.andExpect(status().isOk())
				.andExpect(content().bytes(new byte[]{4, 2}));
	}

	@Test
	void missingImageIs404() throws Exception {
		mvc.perform(get("/page/work/gallery/image/nope.jpg")).andExpect(status().isNotFound());
	}

	@Test
	void deletePaintingRetiresTheImageAndRemovesTheEntry() throws Exception {
		when(backup.retireImage("work", "a.jpg")).thenReturn(true);

		mvc.perform(delete("/page/work/gallery/image/a.jpg")).andExpect(status().isOk());

		verify(backup).retireImage("work", "a.jpg");
		assertThat(site.getPage("work").getGallery().getGallery()).containsOnlyKeys("b.jpg");
		verify(backup).backup(site);
	}

	@Test
	void deletingAPaintingThatCantBeRetiredFailsAndKeepsTheEntry() throws Exception {
		when(backup.retireImage("work", "a.jpg")).thenReturn(false);

		mvc.perform(delete("/page/work/gallery/image/a.jpg")).andExpect(status().isInternalServerError());

		assertThat(site.getPage("work").getGallery().getGallery()).containsKey("a.jpg");
		verify(backup, never()).backup(any());
	}
}
