package es.burl.cms.data;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.burl.cms.data.ImageUploadDTO;
import org.junit.jupiter.api.Test;

import static es.burl.cms.TestSites.*;
import static org.assertj.core.api.Assertions.assertThat;

/** The JSON backups are the only copy of the content, so the format must round-trip. */
class SiteJsonTest {

	private final ObjectMapper mapper = new ObjectMapper()
			.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

	@Test
	void siteRoundTrips() throws Exception {
		Site site = twoPageSite();
		site.setHomeImage(painting("home.jpg", 0));
		site.getExhibitionRepo().add(exhibition("spring", 1_700_000_000_000L));

		Site restored = mapper.readValue(mapper.writeValueAsString(site), Site.class);

		assertThat(restored).usingRecursiveComparison().isEqualTo(site);
	}

	@Test
	void unknownFieldsAreIgnored() throws Exception {
		String json = "{\"name\":\"Old\",\"somethingRemoved\":true,\"pages\":{}}";

		Site site = mapper.readValue(json, Site.class);

		assertThat(site.getName()).isEqualTo("Old");
	}

	@Test
	void missingFieldsGetDefaults() throws Exception {
		Site site = mapper.readValue("{\"name\":\"Minimal\"}", Site.class);

		assertThat(site.getPages()).isEmpty();
		assertThat(site.getHomeImage()).isNotNull();
		assertThat(site.getExhibitionRepo().getMenuItem().getUrl()).isEqualTo("exhibitions");
	}

	@Test
	void galleryEditorPayloadParses() throws Exception {
		String json = """
				{"gallery":{"a.jpg":{"title":"A","filename":"a.jpg","dimensions":"1x1","medium":"Ink","sold":true,"order":4}}}
				""";

		Gallery gallery = mapper.readValue(json, Gallery.class);

		Painting a = gallery.getPainting("a.jpg");
		assertThat(a.getTitle()).isEqualTo("A");
		assertThat(a.isSold()).isTrue();
		assertThat(a.getOrder()).isEqualTo(4);
	}

	@Test
	void uploadPayloadParses() throws Exception {
		String json = """
				{"images":[{"title":"A","filename":"a.png","dimensions":"1x1","medium":"Ink","sold":false,"order":0,"imageData":"AAEC"}]}
				""";

		ImageUploadDTO dto = mapper.readValue(json, ImageUploadDTO.class);

		assertThat(dto.getImages()).singleElement()
				.satisfies(i -> {
					assertThat(i.getFilename()).isEqualTo("a.png");
					assertThat(i.getImageData()).isEqualTo("AAEC");
				});
	}
}
