package es.burl.cms.backup;

import es.burl.cms.data.Exhibition;
import es.burl.cms.data.Page;
import es.burl.cms.data.Site;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static es.burl.cms.TestSites.*;
import static org.assertj.core.api.Assertions.assertThat;

class VersionDiffTest {

	@Test
	void theSameSiteHasNoDifferences() {
		assertThat(VersionDiff.between(twoPageSite(), twoPageSite()).isEmpty()).isTrue();
	}

	@Test
	void pagesAddedRemovedAndChanged() {
		Site from = twoPageSite(), to = twoPageSite();
		to.removePage("about");
		to.addNewPage(page("Contact", "contact", 5));
		to.addNewPage(to.getPage("work").toBuilder().content("<p>Work</p><p>New line</p>").showGallery(false).build());

		VersionDiff diff = VersionDiff.between(from, to);

		assertThat(diff.pages()).extracting(VersionDiff.PageChange::url, VersionDiff.PageChange::kind).containsExactlyInAnyOrder(
				org.assertj.core.groups.Tuple.tuple("about", VersionDiff.Kind.REMOVED),
				org.assertj.core.groups.Tuple.tuple("contact", VersionDiff.Kind.ADDED),
				org.assertj.core.groups.Tuple.tuple("work", VersionDiff.Kind.CHANGED));
		VersionDiff.PageChange work = diff.pages().stream().filter(p -> p.url().equals("work")).findFirst().orElseThrow();
		assertThat(work.details()).containsExactly("Gallery hidden");
		assertThat(work.text()).extracting(VersionDiff.Line::op, VersionDiff.Line::text)
				.containsExactly(org.assertj.core.groups.Tuple.tuple(" ", "Work"), org.assertj.core.groups.Tuple.tuple("+", "New line"));
	}

	@Test
	void paintingChangesAreListedWithTheirDetails() {
		Site from = twoPageSite(), to = twoPageSite();
		Page work = to.getPage("work");
		work.removePainting("b.jpg");
		work.addPaintingToGallery(painting("c.jpg", 5));
		work.getGallery().getGallery().put("a.jpg", work.getGallery().getPainting("a.jpg").toBuilder().sold(true).medium("Watercolour").build());

		List<VersionDiff.PaintingChange> changes = VersionDiff.between(from, to).pages().get(0).paintings();

		assertThat(changes).extracting(VersionDiff.PaintingChange::filename, VersionDiff.PaintingChange::kind, VersionDiff.PaintingChange::side)
				.containsExactly(
						org.assertj.core.groups.Tuple.tuple("c.jpg", VersionDiff.Kind.ADDED, "to"),
						org.assertj.core.groups.Tuple.tuple("b.jpg", VersionDiff.Kind.REMOVED, "from"),
						org.assertj.core.groups.Tuple.tuple("a.jpg", VersionDiff.Kind.CHANGED, "to"));
		assertThat(changes.get(2).details()).contains("Marked sold").anyMatch(d -> d.startsWith("Medium:"));
	}

	@Test
	void menuOrderHomeImageAndExhibitions() {
		Site from = twoPageSite(), to = twoPageSite();
		to.updatePageOrder(Map.of(0, "work", 1, "exhibitions", 2, "about"));
		to.setHomeImage(painting("new-home.jpg", 0));
		to.getExhibitionRepo().add(Exhibition.builder().url("show").title("Show").date(new Date(0)).content("<p>Hi</p>").build());

		VersionDiff diff = VersionDiff.between(from, to);

		assertThat(diff.menuReordered()).isTrue();
		assertThat(diff.homeImage().to()).isEqualTo("new-home.jpg");
		assertThat(diff.exhibitions()).extracting(VersionDiff.ExhibitionChange::kind).containsExactly(VersionDiff.Kind.ADDED);
	}

	@Test
	void textIsComparedWithoutMarkup() {
		assertThat(VersionDiff.text("<p class=\"x\"><b>One</b> &amp; two</p><p><br></p><ul><li>Three</li></ul>"))
				.containsExactly("One & two", "Three");
	}
}
