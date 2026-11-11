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

	@org.junit.jupiter.params.ParameterizedTest
	@org.junit.jupiter.params.provider.CsvSource(delimiter = '|', value = {
			"<p>Hi</p><script>alert(1)</script>                         | alert",
			"<p onclick=\"steal()\">Hi</p>                             | steal",
			"<a href=\"javascript:alert(1)\">x</a>                      | javascript",
			"<a href=\"data:text/html,<script>1</script>\">x</a>        | data:",
			"<img src=\"x\" onerror=\"alert(1)\">                      | onerror",
			"<iframe src=\"https://evil.example/\"></iframe>            | evil.example",
			"<form action=\"https://evil.example/\"><input></form>      | form",
			"<style>body{display:none}</style><p>x</p>                  | display",
			"<p style=\"background:url(javascript:alert(1))\">x</p>     | javascript",
			"<object data=\"x.swf\"></object>                          | object"})
	void dangerousContentIsRemoved(String html, String gone) {
		assertThat(Html.clean(html)).doesNotContainIgnoringCase(gone);
	}

	@Test
	void theEditorsFormattingIsKept() {
		String html = "<p class=\"ql-align-center\" align=\"center\" lang=\"en-GB\"><span style=\"font-size: 18px; color: rgb(0, 0, 0); font-family: Arial\">Selected for <b>WALES</b></span></p>"
				+ "<h2 style=\"text-align: center\">Shows</h2><ul><li><i>one</i></li></ul>"
				+ "<table class=\"table table-bordered\" width=\"100%\"><tbody><tr><td colspan=\"2\">cell</td></tr></tbody></table>"
				+ "<a href=\"https://www.artistsathome.co.uk/artist/52\" target=\"_blank\">link</a>"
				+ "<img src=\"data:image/png;base64,iVBORw0KGgo=\" alt=\"pic\" style=\"width: 50%\">"
				+ "<iframe src=\"//www.youtube.com/embed/abc123\" width=\"640\" height=\"360\" frameborder=\"0\"></iframe>";

		String clean = Html.clean(html);

		assertThat(clean).contains("class=\"ql-align-center\"", "align=\"center\"", "lang=\"en-GB\"", "font-size:18px", "<b>WALES</b>",
				"text-align:center", "<li><i>one</i></li>", "colspan=\"2\"", "width=\"100%\"",
				"href=\"https://www.artistsathome.co.uk/artist/52\"", "target=\"_blank\"",
				"src=\"data:image/png;base64,iVBORw0KGgo&#61;\"", "src=\"//www.youtube.com/embed/abc123\"");
	}

	@Test
	void everyLiveTagSurvives() throws Exception {
		// The tags, attributes and styles the live CV and exhibitions use (2026-11-11)
		String live = "<p class=\"x\" align=\"center\" lang=\"en\" style=\"margin-bottom: 0px; margin-top: 2px; line-height: 1.5\">"
				+ "<span style=\"font-weight: bold; background-color: rgb(255,255,0); letter-spacing: 1px; font-family: Helvetica\">a</span><br>"
				+ "<strong>b</strong></p><div class=\"y\">c</div><table class=\"t\" width=\"600\" style=\"border: 1px solid; outline: 0\"><tbody><tr><td>d</td></tr></tbody></table>";
		String clean = Html.clean(live);
		for (String kept : new String[]{"margin-bottom:0px", "margin-top:2px", "line-height:1.5", "font-weight:bold", "background-color:rgb( 255 , 255 , 0 )",
				"letter-spacing:1px", "font-family:", "<strong>b</strong>", "<div class=\"y\">", "width=\"600\"", "border:1px solid", "<br />"}) {
			assertThat(clean).as(kept).contains(kept);
		}
	}
}
