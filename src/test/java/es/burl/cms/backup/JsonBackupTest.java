package es.burl.cms.backup;

import es.burl.cms.data.Site;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static es.burl.cms.TestSites.twoPageSite;
import static org.assertj.core.api.Assertions.assertThat;

class JsonBackupTest {

	@TempDir
	Path dir;

	private JsonBackup backups(int published, int unpublished) {
		return new JsonBackup(dir, "site-data-backup", published, unpublished);
	}

	private static Site named(String name) {
		return Site.builder().name(name).build();
	}

	private List<String> files(String kind) throws IOException {
		try (Stream<Path> s = Files.list(dir)) {
			return s.map(p -> p.getFileName().toString())
					.filter(n -> n.contains("-" + kind + "-"))
					.sorted()
					.toList();
		}
	}

	@Test
	void backupWritesNumberedUnpublishedFile() throws IOException {
		backups(5, 10).backup(named("A"));

		assertThat(files("unpublished")).singleElement()
				.asString()
				.matches("1-unpublished-\\d{14}-site-data-backup\\.json");
		assertThat(files("published")).isEmpty();
	}

	@Test
	void publishWritesNumberedPublishedFile() throws IOException {
		backups(5, 10).publish(named("A"));

		assertThat(files("published")).singleElement()
				.asString()
				.matches("1-published-\\d{14}-site-data-backup\\.json");
		assertThat(files("unpublished")).isEmpty();
	}

	@Test
	void olderBackupsAreShiftedDown() throws IOException {
		JsonBackup backup = backups(5, 10);
		backup.backup(named("first"));
		backup.backup(named("second"));
		backup.backup(named("third"));

		assertThat(files("unpublished")).extracting(n -> n.substring(0, 1)).containsExactly("1", "2", "3");
		assertThat(backup.buildSiteFromPath(find("1-unpublished")).getName()).isEqualTo("third");
		assertThat(backup.buildSiteFromPath(find("3-unpublished")).getName()).isEqualTo("first");
	}

	@Test
	void rotationKeepsTheConfiguredNumber() throws IOException {
		JsonBackup backup = backups(2, 3);
		for (int i = 0; i < 6; i++) backup.backup(named("u" + i));
		for (int i = 0; i < 4; i++) backup.publish(named("p" + i));

		assertThat(files("unpublished")).hasSize(3);
		assertThat(files("published")).hasSize(2);
		assertThat(backup.buildSiteFromPath(find("3-unpublished")).getName()).isEqualTo("u3");
		assertThat(backup.buildSiteFromPath(find("2-published")).getName()).isEqualTo("p2");
	}

	@Test
	void publishAndBackupRotateIndependently() throws IOException {
		JsonBackup backup = backups(5, 10);
		backup.publish(named("pub"));
		backup.backup(named("draft1"));
		backup.backup(named("draft2"));

		assertThat(files("published")).hasSize(1);
		assertThat(files("unpublished")).hasSize(2);
	}

	@Test
	void restoreReturnsTheNewestDraft() {
		JsonBackup backup = backups(5, 10);
		backup.publish(named("published"));
		backup.backup(named("older draft"));
		backup.backup(named("newest draft"));

		assertThat(backup.restore().getName()).isEqualTo("newest draft");
	}

	@Test
	void restoreFallsBackToPublishedWhenNoDrafts() {
		JsonBackup backup = backups(5, 10);
		backup.publish(named("old pub"));
		backup.publish(named("new pub"));

		assertThat(backup.restore().getName()).isEqualTo("new pub");
	}

	@Test
	void restoreFallsBackToTheNextDraftWhenTheNewestIsMissing() throws IOException {
		JsonBackup backup = backups(5, 10);
		backup.backup(named("older"));
		backup.backup(named("newer"));
		Files.delete(find("1-unpublished"));

		assertThat(backup.restore().getName()).isEqualTo("older");
	}

	@Test
	void restoreOfEmptyFolderIsNull() {
		assertThat(backups(5, 10).restore()).isNull();
	}

	@Test
	void restoredSiteMatchesWhatWasSaved() {
		JsonBackup backup = backups(5, 10);
		Site site = twoPageSite();

		backup.backup(site);

		assertThat(backup.restore()).usingRecursiveComparison().isEqualTo(site);
	}

	@Test
	void backupCreatesAMissingBackupFolder() throws IOException {
		Path missing = dir.resolve("cms/backup");
		JsonBackup backup = new JsonBackup(missing, "site-data-backup", 5, 10);

		backup.backup(named("fresh install"));
		backup.publish(named("fresh install"));

		try (Stream<Path> s = Files.list(missing)) {
			assertThat(s).hasSize(2);
		}
		assertThat(backup.restore().getName()).isEqualTo("fresh install");
	}

	private Path find(String prefix) throws IOException {
		try (Stream<Path> s = Files.list(dir)) {
			return s.filter(p -> p.getFileName().toString().startsWith(prefix)).findFirst().orElseThrow();
		}
	}
}
