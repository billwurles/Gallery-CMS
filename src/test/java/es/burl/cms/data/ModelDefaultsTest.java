package es.burl.cms.data;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ModelDefaultsTest {

	@Test
	void paintingDefaults() {
		Painting painting = Painting.builder().build();

		assertThat(painting.getTitle()).isEmpty();
		assertThat(painting.getFilename()).isEmpty();
		assertThat(painting.getDimensions()).isEmpty();
		assertThat(painting.getMedium()).isEmpty();
		assertThat(painting.isSold()).isFalse();
		assertThat(painting.getOrder()).isEqualTo(100);
	}

	@Test
	void paintingToBuilderKeepsOtherFields() {
		Painting painting = Painting.builder().title("T").filename("f.jpg").sold(true).order(3).build();

		Painting renamed = painting.toBuilder().filename("g.jpg").build();

		assertThat(renamed.getTitle()).isEqualTo("T");
		assertThat(renamed.isSold()).isTrue();
		assertThat(renamed.getOrder()).isEqualTo(3);
		assertThat(renamed.getFilename()).isEqualTo("g.jpg");
	}

	@Test
	void menuItemDefaultsToUnordered() {
		MenuItem item = MenuItem.builder().build();

		assertThat(item.getOrder()).isEqualTo(Integer.MAX_VALUE);
		assertThat(item.getTitle()).isEmpty();
		assertThat(item.getUrl()).isEmpty();
	}

	@Test
	void exhibitionDefaults() {
		Exhibition exhibition = Exhibition.builder().build();

		assertThat(exhibition.getTitle()).isEqualTo("New Exhibition");
		assertThat(exhibition.getUrl()).isEqualTo("new-exhibition");
		assertThat(exhibition.getDate()).isNotNull();
		assertThat(exhibition.getContent()).isEmpty();
	}
}
