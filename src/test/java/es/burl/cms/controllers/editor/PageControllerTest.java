package es.burl.cms.controllers.editor;

import es.burl.cms.backup.BackupSite;
import es.burl.cms.data.Page;
import es.burl.cms.data.Site;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.file.Files;
import java.nio.file.Path;

import static es.burl.cms.TestSites.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PageControllerTest {

	@TempDir
	Path galleryRoot;

	Site site;
	BackupSite backup;
	MockMvc mvc;

	@BeforeEach
	void setUp() {
		site = twoPageSite();
		backup = mock(BackupSite.class);
		mvc = MockMvcBuilders.standaloneSetup(new PageController(site, galleryRoot, backup))
				.setControllerAdvice(new EditorAdvice(site)).build();
	}

	@Test
	void newPageForm() throws Exception {
		mvc.perform(get("/page"))
				.andExpect(status().isOk())
				.andExpect(view().name("editor/EditPage"))
				.andExpect(model().attribute("newPage", true))
				.andExpect(model().attributeExists("page", "menuItems", "message"));
	}

	@Test
	void createPage() throws Exception {
		mvc.perform(post("/page").param("title", "Contact").param("url", "contact").param("content", "<p>Hi</p>"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/page/contact"));

		Page created = site.getPage("contact");
		assertThat(created.getTitle()).isEqualTo("Contact");
		assertThat(created.getContent()).isEqualTo("<p>Hi</p>");
		assertThat(created.getMenuItem().getOrder()).isEqualTo(3);
		verify(backup).backup(site);
	}

	@Test
	void createPageWithTakenUrlShowsAnError() throws Exception {
		mvc.perform(post("/page").param("title", "Another").param("url", "about").param("content", "x"))
				.andExpect(status().isOk())
				.andExpect(view().name("editor/EditPage"))
				.andExpect(model().attribute("message", "Page url (about) is already in use"))
				.andExpect(model().attribute("newPage", true));

		assertThat(site.getPage("about").getTitle()).isEqualTo("About");
		verifyNoInteractions(backup);
	}

	@Test
	void createPageCannotTakeTheExhibitionsUrl() throws Exception {
		mvc.perform(post("/page").param("title", "X").param("url", "exhibitions").param("content", "x"))
				.andExpect(view().name("editor/EditPage"));

		assertThat(site.getPage("exhibitions")).isNull();
	}

	@Test
	void editPage() throws Exception {
		mvc.perform(get("/page/about"))
				.andExpect(status().isOk())
				.andExpect(view().name("editor/EditPage"))
				.andExpect(model().attribute("page", site.getPage("about")))
				.andExpect(model().attribute("newPage", false))
				.andExpect(model().attribute("hasGallery", false));
	}

	@Test
	void editPageWithTrailingSlash() throws Exception {
		mvc.perform(get("/page/about/")).andExpect(view().name("editor/EditPage"));
	}

	@Test
	void galleryOnlyPageRedirectsToTheGalleryEditor() throws Exception {
		Page gallery = page("Prints", "prints", 5).toBuilder().content("  ").build();
		gallery.addPaintingToGallery(painting("p.jpg", 0));
		site.addNewPage(gallery);

		mvc.perform(get("/page/prints")).andExpect(redirectedUrl("/page/prints/gallery"));
	}

	@Test
	void unknownPageIs404View() throws Exception {
		mvc.perform(get("/page/missing"))
				.andExpect(status().isNotFound())
				.andExpect(view().name("editor/NotFound"))
				.andExpect(model().attribute("message", "The page missing was not found"));
	}

	@Test
	void saveContentInPlace() throws Exception {
		mvc.perform(post("/page/about").param("title", "About Us").param("url", "about").param("content", "<p>New</p>"))
				.andExpect(redirectedUrl("/page/about"));

		Page about = site.getPage("about");
		assertThat(about.getTitle()).isEqualTo("About Us");
		assertThat(about.getContent()).isEqualTo("<p>New</p>");
		assertThat(about.getMenuItem().getOrder()).isZero();
		verify(backup).backup(site);
	}

	@Test
	void renameMovesThePageAndItsImages() throws Exception {
		Path oldDir = Files.createDirectories(galleryRoot.resolve("work"));
		Files.write(oldDir.resolve("a.jpg"), new byte[]{1});
		Files.write(oldDir.resolve("b.jpg"), new byte[]{2});

		mvc.perform(post("/page/work").param("title", "Paintings").param("url", "paintings").param("content", "c"))
				.andExpect(redirectedUrl("/page/paintings"));

		assertThat(site.getPage("work")).isNull();
		Page renamed = site.getPage("paintings");
		assertThat(renamed.getTitle()).isEqualTo("Paintings");
		assertThat(renamed.getMenuItem().getOrder()).isEqualTo(2);
		assertThat(renamed.getGallery().getGallery()).containsOnlyKeys("a.jpg", "b.jpg");
		assertThat(galleryRoot.resolve("paintings/a.jpg")).hasBinaryContent(new byte[]{1});
		assertThat(galleryRoot.resolve("paintings/b.jpg")).hasBinaryContent(new byte[]{2});
		assertThat(oldDir).doesNotExist(); // moved, not copied: no duplicates for the cleanup to find
		verify(backup).backup(site);
	}

	@Test
	void renameOntoATakenUrlShowsAnError() throws Exception {
		mvc.perform(post("/page/work").param("title", "Work").param("url", "about").param("content", "c"))
				.andExpect(view().name("editor/EditPage"))
				.andExpect(model().attribute("message", "Page URL about is already in use"));

		assertThat(site.getPage("work")).isNotNull();
		assertThat(site.getPage("about").getTitle()).isEqualTo("About");
		verifyNoInteractions(backup);
	}

	@Test
	void deletePageRenumbersTheMenu() throws Exception {
		mvc.perform(delete("/page/about"))
				.andExpect(status().isOk())
				.andExpect(content().string("Page about was deleted"));

		assertThat(site.getPage("about")).isNull();
		assertThat(site.getMenuItems()).extracting(m -> m.getUrl() + "=" + m.getOrder())
				.containsExactly("exhibitions=0", "work=1");
		verify(backup).backup(site);
	}

	@Test
	void aPageWithoutAGalleryOpensItsEditor() throws Exception {
		site.addNewPage(site.getPage("about").toBuilder().gallery(null).content(null).build());

		mvc.perform(get("/page/about")).andExpect(view().name("editor/EditPage"));
	}

	@Test
	void savingAnUnknownPageIs404() throws Exception {
		mvc.perform(post("/page/missing").param("title", "X").param("url", "missing").param("content", "c"))
				.andExpect(status().isNotFound())
				.andExpect(view().name("editor/NotFound"));

		verifyNoInteractions(backup);
	}

	@Test
	void deletingAnUnknownPageIs404() throws Exception {
		mvc.perform(delete("/page/missing")).andExpect(status().isNotFound());

		verifyNoInteractions(backup);
	}

	@Test
	void deletingAPageLeavesItsGalleryFolderForTheCleanup() throws Exception {
		Path dir = Files.createDirectories(galleryRoot.resolve("work"));
		Files.write(dir.resolve("a.jpg"), new byte[]{1});

		mvc.perform(delete("/page/work")).andExpect(status().isOk());

		assertThat(dir.resolve("a.jpg")).exists();
	}
}
