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
				.matches("1-unpublished-\\d{17}-site-data-backup\\.json");
		assertThat(files("published")).isEmpty();
	}

	@Test
	void publishWritesNumberedPublishedFile() throws IOException {
		backups(5, 10).publish(named("A"));

		assertThat(files("published")).singleElement()
				.asString()
				.matches("1-published-\\d{17}-site-data-backup\\.json");
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

	// ---- BUG-6 regressions

	@Test
	void filesThatAreNotBackupsAreIgnored() throws IOException {
		Files.writeString(dir.resolve("notes-published.txt"), "not a backup");
		Files.writeString(dir.resolve("1-published-12000020250101-other-name.json"), "{}");
		Files.writeString(dir.resolve("x-unpublished-abc-site-data-backup.json"), "{}");
		Files.createDirectories(dir.resolve("images"));
		JsonBackup backup = backups(5, 10);

		backup.backup(named("draft"));
		backup.publish(named("pub"));

		assertThat(backup.restore().getName()).isEqualTo("draft");
		assertThat(dir.resolve("notes-published.txt")).exists();
		assertThat(dir.resolve("1-published-12000020250101-other-name.json")).exists();
	}

	@Test
	void restoreFindsTheOldestBackupWhenItIsTheOnlyOne() throws IOException {
		JsonBackup backup = backups(5, 3);
		backup.backup(named("only one"));
		Path first = find("1-unpublished");
		Files.move(first, dir.resolve(first.getFileName().toString().replaceFirst("^1-", "3-")));

		assertThat(backup.restore().getName()).isEqualTo("only one");
	}

	@Test
	void restoreSkipsACorruptNewestBackup() throws IOException {
		JsonBackup backup = backups(5, 10);
		backup.backup(named("good"));
		backup.backup(named("will be corrupted"));
		Files.writeString(find("1-unpublished"), "{\"name\": \"trunc");

		assertThat(backup.restore().getName()).isEqualTo("good");
	}

	@Test
	void restoreFallsBackToPublishedWhenEveryDraftIsCorrupt() throws IOException {
		JsonBackup backup = backups(5, 10);
		backup.publish(named("published"));
		backup.backup(named("draft"));
		Files.writeString(find("1-unpublished"), "garbage");

		assertThat(backup.restore().getName()).isEqualTo("published");
	}

	@Test
	void rotationDropsBackupsBeyondALoweredLimit() throws IOException {
		JsonBackup generous = backups(5, 10);
		for (int i = 0; i < 6; i++) generous.backup(named("u" + i));

		backups(5, 3).backup(named("after lowering"));

		assertThat(files("unpublished")).hasSize(3);
		assertThat(backups(5, 3).restore().getName()).isEqualTo("after lowering");
	}

	@Test
	void noTempFilesAreLeftBehind() throws IOException {
		JsonBackup backup = backups(5, 10);
		backup.backup(named("a"));
		backup.publish(named("b"));

		try (Stream<Path> s = Files.list(dir)) {
			assertThat(s.map(p -> p.getFileName().toString())).noneMatch(n -> n.startsWith("."));
		}
	}

	@Test
	void backupsGetTheSamePermissionsAsAnyOtherNewFile() throws IOException {
		JsonBackup backup = backups(5, 10);
		backup.backup(named("a"));
		backup.publish(named("b"));

		// Regression: writing through Files.createTempFile left backups readable by their owner only
		var expected = Files.getPosixFilePermissions(Files.createFile(dir.resolve("ordinary-file")));
		assertThat(Files.getPosixFilePermissions(find("1-unpublished"))).isEqualTo(expected);
		assertThat(Files.getPosixFilePermissions(find("1-published"))).isEqualTo(expected);
	}

	private Path find(String prefix) throws IOException {
		try (Stream<Path> s = Files.list(dir)) {
			return s.filter(p -> p.getFileName().toString().startsWith(prefix)).findFirst().orElseThrow();
		}
	}

	@Test
	void neverPublishedMeansChanged() {
		JsonBackup b = backups(5, 10);
		Site site = twoPageSite();
		b.backup(site);

		JsonBackup.PublishStatus status = b.status(site);

		assertThat(status.changed()).isTrue();
		assertThat(status.lastPublished()).isNull();
		assertThat(status.changedSince()).isNotNull();
	}

	@Test
	void publishingClearsTheChangesUntilTheNextEdit() {
		JsonBackup b = backups(5, 10);
		Site site = twoPageSite();
		b.backup(site);
		b.publish(site);

		JsonBackup.PublishStatus published = b.status(site);
		assertThat(published.changed()).isFalse();
		assertThat(published.lastPublished()).isNotNull();
		assertThat(published.changedSince()).isNull();

		site.addNewPage(es.burl.cms.TestSites.page("New", "new", 5));
		b.backup(site);
		JsonBackup.PublishStatus edited = b.status(site);
		assertThat(edited.changed()).isTrue();
		assertThat(edited.lastPublished()).isEqualTo(published.lastPublished());
		assertThat(edited.changedSince()).isNotNull();

		site.removePage("new");
		assertThat(b.status(site).changed()).as("back to what's live").isFalse();
	}
}
