package es.burl.cms.data;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;

import static es.burl.cms.TestSites.*;
import static org.assertj.core.api.Assertions.assertThat;

class PageTest {

	@Test
	void defaults() {
		Page page = Page.builder().build();

		assertThat(page.getContent()).isNull();
		assertThat(page.getGallery()).isNotNull();
		assertThat(page.getGallery().isEmpty()).isTrue();
		assertThat(page.getMenuItem().getOrder()).isEqualTo(Integer.MAX_VALUE);
	}

	@Test
	void urlAndTitleComeFromTheMenuItem() {
		Page page = page("About Me", "about", 3);

		assertThat(page.getUrl()).isEqualTo("about");
		assertThat(page.getTitle()).isEqualTo("About Me");
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = {"", "   ", "\n\t "})
	void blankContentIsNotContent(String content) {
		Page page = Page.builder().content(content).build();

		assertThat(page.hasContent()).isFalse();
	}

	@Test
	void textIsContent() {
		assertThat(Page.builder().content(" <p>hi</p> ").build().hasContent()).isTrue();
	}

	@Test
	void hasGalleryOnlyWithPaintings() {
		Page page = Page.builder().build();
		assertThat(page.hasGallery()).isFalse();

		page.addPaintingToGallery(painting("a.jpg", 0));
		assertThat(page.hasGallery()).isTrue();

		page.setGallery(null);
		assertThat(page.hasGallery()).isFalse();
	}

	@Test
	void saveGalleryReplacesTheGallery() {
		Page page = page("Work", "work", 0);
		page.addPaintingToGallery(painting("old.jpg", 0));
		Gallery replacement = Gallery.builder().build();
		replacement.addPainting(painting("new.jpg", 0));

		assertThat(page.saveGallery(replacement, Path.of("unused"))).isTrue();

		assertThat(page.getGallery()).isSameAs(replacement);
	}

	@Test
	void saveGalleryRejectsNull() {
		Page page = page("Work", "work", 0);
		page.addPaintingToGallery(painting("keep.jpg", 0));

		assertThat(page.saveGallery(null, Path.of("unused"))).isFalse();

		assertThat(page.getGallery().getGallery()).containsOnlyKeys("keep.jpg");
	}

	@Test
	void removePainting() {
		Page page = page("Work", "work", 0);
		page.addPaintingToGallery(painting("a.jpg", 0));
		page.addPaintingToGallery(painting("b.jpg", 1));

		page.removePainting("a.jpg");

		assertThat(page.getGallery().getGallery()).containsOnlyKeys("b.jpg");
	}
}
