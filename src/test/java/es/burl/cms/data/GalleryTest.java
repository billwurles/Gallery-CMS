package es.burl.cms.data;

import org.junit.jupiter.api.Test;

import static es.burl.cms.TestSites.painting;
import static org.assertj.core.api.Assertions.assertThat;

class GalleryTest {

	@Test
	void emptyByDefault() {
		Gallery gallery = Gallery.builder().build();

		assertThat(gallery.isEmpty()).isTrue();
		assertThat(gallery.getGalleryInOrder()).isEmpty();
	}

	@Test
	void paintingsAreKeyedByFilename() {
		Gallery gallery = Gallery.builder().build();
		Painting painting = painting("a.jpg", 0);

		gallery.addPainting(painting);

		assertThat(gallery.getPainting("a.jpg")).isSameAs(painting);
		assertThat(gallery.getPainting("missing.jpg")).isNull();
	}

	@Test
	void addingTheSameFilenameReplaces() {
		Gallery gallery = Gallery.builder().build();
		gallery.addPainting(painting("a.jpg", 0));
		Painting updated = painting("a.jpg", 0).toBuilder().title("Updated").build();

		gallery.addPainting(updated);

		assertThat(gallery.getGallery()).hasSize(1);
		assertThat(gallery.getPainting("a.jpg").getTitle()).isEqualTo("Updated");
	}

	@Test
	void galleryInOrderSortsByOrder() {
		Gallery gallery = Gallery.builder().build();
		gallery.addPainting(painting("c.jpg", 2));
		gallery.addPainting(painting("a.jpg", 0));
		gallery.addPainting(painting("b.jpg", 1));

		assertThat(gallery.getGalleryInOrder()).extracting(Painting::getFilename)
				.containsExactly("a.jpg", "b.jpg", "c.jpg");
	}
}
