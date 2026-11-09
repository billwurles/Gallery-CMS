package es.burl.cms;

import es.burl.cms.data.*;

import java.util.Date;

/** Small builders for test fixtures. */
public final class TestSites {

	private TestSites() {}

	public static MenuItem menuItem(String title, String url, int order) {
		return MenuItem.builder().title(title).url(url).order(order).build();
	}

	public static Page page(String title, String url, int order) {
		return Page.builder().menuItem(menuItem(title, url, order)).content("<p>" + title + "</p>").build();
	}

	public static Painting painting(String filename, int order) {
		return Painting.builder()
				.title("Title " + filename)
				.filename(filename)
				.dimensions("10x20cm")
				.medium("Oil")
				.order(order)
				.build();
	}

	public static Exhibition exhibition(String url, long epochMillis) {
		return Exhibition.builder()
				.title("Exhibition " + url)
				.url(url)
				.date(new Date(epochMillis))
				.content("<p>About " + url + "</p>")
				.build();
	}

	/** A site with two pages (one with a two-painting gallery) and no exhibitions. */
	public static Site twoPageSite() {
		Site site = Site.builder().name("Test Site").build();
		Page about = page("About", "about", 0);
		Page work = page("Work", "work", 2);
		work.addPaintingToGallery(painting("b.jpg", 1));
		work.addPaintingToGallery(painting("a.jpg", 0));
		site.addNewPage(about);
		site.addNewPage(work);
		return site;
	}
}
