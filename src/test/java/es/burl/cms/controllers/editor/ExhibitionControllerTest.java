package es.burl.cms.controllers.editor;

import es.burl.cms.backup.BackupSite;
import es.burl.cms.data.Exhibition;
import es.burl.cms.data.Site;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static es.burl.cms.TestSites.exhibition;
import static es.burl.cms.TestSites.twoPageSite;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ExhibitionControllerTest {

	Site site;
	BackupSite backup;
	MockMvc mvc;

	@BeforeEach
	void setUp() {
		site = twoPageSite();
		for (int i = 0; i < 9; i++) site.getExhibitionRepo().add(exhibition("ex-" + i, i * 86_400_000L));
		backup = mock(BackupSite.class);
		mvc = MockMvcBuilders.standaloneSetup(new ExhibitionController(site, backup))
				.setControllerAdvice(new EditorAdvice(site)).build();
	}

	@Test
	void listFirstPage() throws Exception {
		mvc.perform(get("/exhibitions"))
				.andExpect(status().isOk())
				.andExpect(view().name("editor/ViewExhibitions"))
				.andExpect(model().attribute("page", 1))
				.andExpect(model().attribute("totalPages", 2))
				.andExpect(model().attribute("exhibitionsPage", hasSize(7)));
	}

	@Test
	void listSecondPage() throws Exception {
		mvc.perform(get("/exhibitions/").param("page", "2"))
				.andExpect(model().attribute("page", 2))
				.andExpect(model().attribute("exhibitionsPage", hasSize(2)));
	}

	@Test
	void newExhibitionForm() throws Exception {
		mvc.perform(get("/exhibitions/new"))
				.andExpect(view().name("editor/EditExhibition"))
				.andExpect(model().attribute("exhibition", instanceOf(Exhibition.class)));
	}

	@Test
	void editExistingExhibition() throws Exception {
		mvc.perform(get("/exhibitions/ex-3"))
				.andExpect(view().name("editor/EditExhibition"))
				.andExpect(model().attribute("exhibition", site.getExhibitionRepo().get("ex-3")));
	}

	@Test
	void saveExhibition() throws Exception {
		String json = """
				{"title":"Summer Show","url":"summer-show","date":1700000000000,"content":"<p>Opening</p>"}""";

		mvc.perform(post("/exhibitions/new/save").contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Summer Show"));

		Exhibition saved = site.getExhibitionRepo().get("summer-show");
		assertThat(saved.getContent()).isEqualTo("<p>Opening</p>");
		assertThat(saved.getDate().getTime()).isEqualTo(1_700_000_000_000L);
		verify(backup).backup(site);
	}

	@Test
	void saveExhibitionOverwritesSameUrl() throws Exception {
		String json = """
				{"title":"Updated","url":"ex-3","date":0,"content":""}""";

		mvc.perform(post("/exhibitions/ex-3/save").contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isOk());

		assertThat(site.getExhibitionRepo().get("ex-3").getTitle()).isEqualTo("Updated");
		assertThat(site.getExhibitionRepo().getExhibitions()).hasSize(9);
	}

	@Test
	void deleteExhibition() throws Exception {
		mvc.perform(delete("/exhibitions/ex-3")).andExpect(status().isOk());

		assertThat(site.getExhibitionRepo().get("ex-3")).isNull();
		verify(backup).backup(site);
	}

	@Test
	void editingAnUnknownExhibitionIs404() throws Exception {
		mvc.perform(get("/exhibitions/nope"))
				.andExpect(status().isNotFound())
				.andExpect(view().name("editor/NotFound"));
	}

	@Test
	void deletingAnUnknownExhibitionIs404() throws Exception {
		mvc.perform(delete("/exhibitions/nope")).andExpect(status().isNotFound());

		verifyNoInteractions(backup);
	}

	@Test
	void changingTheAddressReplacesTheOldEntry() throws Exception {
		int before = site.getExhibitionRepo().getExhibitions().size();
		String json = "{\"url\":\"renamed\",\"title\":\"Renamed\",\"date\":\"2024-05-01T10:00\",\"content\":\"<p>x</p>\"}";

		mvc.perform(post("/exhibitions/ex-3/save").contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isOk());

		assertThat(site.getExhibitionRepo().get("ex-3")).isNull();
		assertThat(site.getExhibitionRepo().get("renamed").getTitle()).isEqualTo("Renamed");
		assertThat(site.getExhibitionRepo().getExhibitions()).hasSize(before);
	}
}
