package es.burl.cms.controllers.editor;

import es.burl.cms.backup.BackupSite;
import es.burl.cms.backup.JsonBackup;
import es.burl.cms.builder.ThymeleafSiteBuilder;
import es.burl.cms.data.MenuItem;
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

import static es.burl.cms.TestSites.painting;
import static es.burl.cms.TestSites.twoPageSite;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class EditorControllerTest {

	@TempDir
	Path galleryRoot;

	Site site;
	BackupSite backup;
	JsonBackup publisher;
	ThymeleafSiteBuilder builder;
	MockMvc mvc;

	@BeforeEach
	void setUp() {
		site = twoPageSite();
		backup = mock(BackupSite.class);
		publisher = mock(JsonBackup.class);
		builder = mock(ThymeleafSiteBuilder.class);
		mvc = MockMvcBuilders.standaloneSetup(new EditorController(site, galleryRoot, backup, publisher, builder)).build();
	}

	@Test
	void dashboard() throws Exception {
		mvc.perform(get("/"))
				.andExpect(status().isOk())
				.andExpect(view().name("editor/EditorHome"))
				.andExpect(model().attribute("name", "Test Site"))
				.andExpect(model().attribute("menuItems", site.getMenuItems()))
				.andExpect(model().attribute("homeImage", site.getHomeImage()));
	}

	@Test
	void publishBuildsThenBacksUp() throws Exception {
		mvc.perform(get("/save"))
				.andExpect(status().isOk())
				.andExpect(content().string("Site published"));

		var order = inOrder(publisher, builder);
		order.verify(builder).buildSite(site);
		order.verify(publisher).publish(site);
	}

	@Test
	void aFailedBuildIsReportedAndNothingIsMarkedPublished() throws Exception {
		doThrow(new java.io.IOException("disk full")).when(builder).buildSite(site);

		mvc.perform(get("/save"))
				.andExpect(status().isInternalServerError())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("live site is unchanged")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("disk full")));

		verify(publisher, never()).publish(any());
	}

	@Test
	void aFailedPublishedBackupIsReported() throws Exception {
		doThrow(new RuntimeException("backup folder missing")).when(publisher).publish(site);

		mvc.perform(get("/save"))
				.andExpect(status().isInternalServerError())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("backup folder missing")));
	}

	@Test
	void servesTheHomeImage() throws Exception {
		Files.write(galleryRoot.resolve("home.jpg"), new byte[]{9});

		mvc.perform(get("/image/home.jpg")).andExpect(content().bytes(new byte[]{9}));
	}

	@Test
	void homeImageUploadPage() throws Exception {
		mvc.perform(get("/upload"))
				.andExpect(view().name("editor/UploadGallery"))
				.andExpect(model().attribute("page", ""));
	}

	@Test
	void uploadingAHomeImageReplacesTheOldOne() throws Exception {
		site.setHomeImage(painting("old-home.jpg", 0));
		String json = """
				{"images":[{"title":"Home","filename":"h.jpg","dimensions":"","medium":"","sold":false,"order":0,"imageData":"%s"}]}"""
				.formatted(Base64.getEncoder().encodeToString(new byte[]{5, 5}));

		mvc.perform(post("/upload").contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isOk());

		verify(backup).retireImage("", "old-home.jpg");
		assertThat(site.getHomeImage().getTitle()).isEqualTo("Home");
		assertThat(galleryRoot.resolve(site.getHomeImage().getFilename())).hasBinaryContent(new byte[]{5, 5});
		verify(backup).backup(site);
	}

	@Test
	void rearrangeMenu() throws Exception {
		mvc.perform(post("/rearrange").contentType(MediaType.APPLICATION_JSON)
						.content("{\"0\":\"work\",\"1\":\"exhibitions\",\"2\":\"about\"}"))
				.andExpect(status().isOk());

		assertThat(site.getMenuItems()).extracting(MenuItem::getUrl).containsExactly("work", "exhibitions", "about");
		verify(backup).backup(site);
	}
}
