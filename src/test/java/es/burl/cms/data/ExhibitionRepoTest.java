package es.burl.cms.data;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static es.burl.cms.TestSites.exhibition;
import static org.assertj.core.api.Assertions.assertThat;

class ExhibitionRepoTest {

	private static ExhibitionRepo repoWith(int count) {
		ExhibitionRepo repo = ExhibitionRepo.builder().build();
		for (int i = 0; i < count; i++) {
			repo.add(exhibition("ex-" + i, i * 86_400_000L)); // ex-0 oldest
		}
		return repo;
	}

	@Test
	void defaults() {
		ExhibitionRepo repo = ExhibitionRepo.builder().build();

		assertThat(repo.getExhibitions()).isEmpty();
		assertThat(repo.getMenuItem().getTitle()).isEqualTo("Exhibitions");
		assertThat(repo.getMenuItem().getUrl()).isEqualTo("exhibitions");
		assertThat(repo.getMenuItem().getOrder()).isEqualTo(1);
	}

	@Test
	void addAndGetByUrl() {
		ExhibitionRepo repo = ExhibitionRepo.builder().build();
		Exhibition exhibition = exhibition("spring", 0);

		repo.add(exhibition);

		assertThat(repo.get("spring")).isSameAs(exhibition);
		assertThat(repo.get("missing")).isNull();
	}

	@Test
	void dateOrderIsNewestFirst() {
		assertThat(repoWith(3).getExhibitionsInDateOrder()).extracting(Exhibition::getUrl)
				.containsExactly("ex-2", "ex-1", "ex-0");
	}

	@ParameterizedTest
	@CsvSource({"0,7,0", "1,7,1", "7,7,1", "8,7,2", "14,7,2", "15,7,3", "5,2,3"})
	void totalPages(int exhibitions, int perPage, int expectedPages) {
		assertThat(repoWith(exhibitions).getTotalPages(perPage)).isEqualTo(expectedPages);
	}

	@Test
	void firstPageHoldsTheNewest() {
		ExhibitionRepo repo = repoWith(10);

		assertThat(repo.getExhibitionsPage(1, 7)).extracting(Exhibition::getUrl)
				.containsExactly("ex-9", "ex-8", "ex-7", "ex-6", "ex-5", "ex-4", "ex-3");
	}

	@Test
	void lastPageIsPartial() {
		ExhibitionRepo repo = repoWith(10);

		assertThat(repo.getExhibitionsPage(2, 7)).extracting(Exhibition::getUrl)
				.containsExactly("ex-2", "ex-1", "ex-0");
	}

	@Test
	void exactlyFullPage() {
		assertThat(repoWith(7).getExhibitionsPage(1, 7)).hasSize(7);
	}

	@Test
	void pagesPastTheEndAreEmpty() {
		assertThat(repoWith(3).getExhibitionsPage(2, 7)).isEmpty();
		assertThat(repoWith(0).getExhibitionsPage(1, 7)).isEmpty();
		assertThat(repoWith(3).getExhibitionsPage(0, 7)).isEmpty();
	}
}
