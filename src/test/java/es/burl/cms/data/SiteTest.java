package es.burl.cms.data;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static es.burl.cms.TestSites.*;
import static org.assertj.core.api.Assertions.assertThat;

class SiteTest {

	@Test
	void defaultsForANewSite() {
		Site site = Site.builder().build();

		assertThat(site.getName()).isEqualTo("Untitled Site");
		assertThat(site.getPages()).isEmpty();
		assertThat(site.getHomeImage()).isNotNull();
		assertThat(site.getExhibitionRepo().getExhibitions()).isEmpty();
		assertThat(site.getExhibitionRepo().getMenuItem().getUrl()).isEqualTo("exhibitions");
	}

	@Test
	void addAndGetPageByUrl() {
		Site site = Site.builder().build();
		Page page = page("About", "about", 0);

		site.addNewPage(page);

		assertThat(site.getPage("about")).isSameAs(page);
		assertThat(site.getPage("missing")).isNull();
	}

	@Test
	void removePageDropsIt() {
		Site site = twoPageSite();

		assertThat(site.removePage("about")).isTrue();

		assertThat(site.getPage("about")).isNull();
		assertThat(site.getPages()).containsOnlyKeys("work");
	}

	@Test
	void menuItemsIncludeExhibitionsAndAreSortedByOrder() {
		Site site = twoPageSite(); // about=0, exhibitions=1 (default), work=2

		List<String> urls = site.getMenuItems().stream().map(MenuItem::getUrl).toList();

		assertThat(urls).containsExactly("about", "exhibitions", "work");
	}

	@Test
	void pagesInOrderExcludesExhibitions() {
		Site site = twoPageSite();

		assertThat(site.getPagesInOrder()).extracting(Page::getUrl).containsExactly("about", "work");
	}

	@Test
	void nextPageOrderIsOneMoreThanTheHighest() {
		assertThat(twoPageSite().getNextPageOrder()).isEqualTo(3);
	}

	@Test
	void nextPageOrderIgnoresUnorderedItems() {
		Site site = twoPageSite();
		site.addNewPage(page("Draft", "draft", Integer.MAX_VALUE));

		assertThat(site.getNextPageOrder()).isEqualTo(3);
	}

	@Test
	void nextPageOrderOnEmptySiteFollowsExhibitions() {
		assertThat(Site.builder().build().getNextPageOrder()).isEqualTo(2);
	}

	@Test
	void updatePageOrderMovesPagesAndExhibitions() {
		Site site = twoPageSite();

		site.updatePageOrder(Map.of(0, "work", 1, "about", 2, "exhibitions"));

		assertThat(site.getMenuItems()).extracting(MenuItem::getUrl)
				.containsExactly("work", "about", "exhibitions");
	}

	@Test
	void reorderPageOrderClosesGaps() {
		Site site = twoPageSite();
		site.addNewPage(page("Late", "late", 10));

		site.reorderPageOrder();

		assertThat(site.getMenuItems()).extracting(MenuItem::getOrder).containsExactly(0, 1, 2, 3);
		assertThat(site.getMenuItems()).extracting(MenuItem::getUrl)
				.containsExactly("about", "exhibitions", "work", "late");
	}

	@Test
	void removeExhibition() {
		Site site = Site.builder().build();
		site.getExhibitionRepo().add(exhibition("spring", 0));

		site.removeExhibition("spring");

		assertThat(site.getExhibitionRepo().getExhibitions()).isEmpty();
	}

	@Test
	void pageGalleryLookup() {
		Site site = twoPageSite();

		assertThat(site.getPageGallery("work").getGallery()).containsOnlyKeys("a.jpg", "b.jpg");
	}
}
