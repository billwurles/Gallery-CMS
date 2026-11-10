package es.burl.cms.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HtmlTest {

	// What the live Recent Work page held: the editor's hidden field, then a stray comma
	static final String SAVED_FIELD = "\r\n                    <textarea name=\"content\" style=\"display:none;\" id=\"content\"></textarea>\r\n                ,";

	@Test
	void theStrayEditorFieldIsRemoved() {
		assertThat(Html.clean(SAVED_FIELD)).doesNotContain("textarea").contains(",");
		assertThat(Html.clean("<p>a</p><TEXTAREA id=x>\nold\n</textarea ><p>b</p>")).isEqualTo("<p>a</p><p>b</p>");
		assertThat(Html.clean(null)).isNull();
	}

	@Test
	void theEditorFieldAloneIsBlank() {
		assertThat(Html.isBlank("<textarea name=\"content\"></textarea>\n<p><br></p>")).isTrue();
	}

	@Test
	void pagesAndExhibitionsFromBackupsAreCleaned() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		String json = mapper.writeValueAsString(SAVED_FIELD);

		Page page = mapper.readValue("{\"content\":" + json + "}", Page.class);
		Exhibition exhibition = mapper.readValue("{\"content\":" + json + "}", Exhibition.class);

		assertThat(page.getContent()).doesNotContain("textarea");
		assertThat(exhibition.getContent()).doesNotContain("textarea");
		assertThat(page.toBuilder().build().getContent()).isEqualTo(page.getContent());
	}
}
