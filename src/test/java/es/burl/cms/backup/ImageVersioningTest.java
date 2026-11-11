package es.burl.cms.backup;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import es.burl.cms.data.Page;
import es.burl.cms.data.Site;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static es.burl.cms.TestSites.*;
import static org.assertj.core.api.Assertions.assertThat;

/** BACKUP-2 (image archive + manifest) and BACKUP-3 (safe deletes + cleanup). */
class ImageVersioningTest {

	@TempDir
	Path tmp;

	Path gallery;
	Path backupDir;
	Site site;
	final ObjectMapper mapper = new ObjectMapper();

	static final byte[] A = {1, 1, 1};
	static final byte[] B = {2, 2, 2, 2};
	static final byte[] HOME = {9};

	@BeforeEach
	void setUp() throws IOException {
		gallery = Files.createDirectories(tmp.resolve("gallery"));
		backupDir = tmp.resolve("backup");
		site = twoPageSite(); // work has a.jpg and b.jpg
		site.setHomeImage(painting("home.jpg", 0));
		Path work = Files.createDirectories(gallery.resolve("work"));
		Files.write(work.resolve("a.jpg"), A);
		Files.write(work.resolve("b.jpg"), B);
		Files.write(gallery.resolve("home.jpg"), HOME);
	}

	JsonBackup backups(int published, int drafts, boolean dryRun) {
		return new JsonBackup(backupDir, "site-data-backup", published, drafts, gallery, dryRun);
	}

	JsonBackup backups() {
		return backups(2, 3, false);
	}

	Path newest(String kind) throws IOException {
		try (Stream<Path> s = Files.list(backupDir)) {
			return s.filter(p -> p.getFileName().toString().startsWith("1-" + kind + "-")).findFirst().orElseThrow();
		}
	}

	JsonNode manifest(Path backupFile) throws IOException {
		return mapper.readTree(backupFile.toFile()).get(JsonBackup.MANIFEST_FIELD);
	}

	List<String> archived() throws IOException {
		Path dir = backupDir.resolve("images");
		if (!Files.isDirectory(dir)) return List.of();
		try (Stream<Path> s = Files.list(dir)) {
			return s.map(p -> p.getFileName().toString()).sorted().toList();
		}
	}

	static String sha(byte[] bytes) throws IOException {
		Path f = Files.createTempFile("sha", ".bin");
		Files.write(f, bytes);
		try {
			return ImageArchive.hashOf(f);
		} finally {
			Files.delete(f);
		}
	}

	// ---- BACKUP-2

	@Test
	void manifestListsEveryImageWithHashAndSize() throws IOException {
		backups().backup(site);

		JsonNode manifest = manifest(newest("unpublished"));
		assertThat(manifest.get("schemaVersion").asInt()).isEqualTo(1);
		List<String> entries = new ArrayList<>();
		manifest.get("images").forEach(i -> entries.add(i.get("path").asText() + " " + i.get("sha256").asText() + " " + i.get("size").asLong()));
		assertThat(entries).containsExactlyInAnyOrder(
				"home.jpg " + sha(HOME) + " 1",
				"work/a.jpg " + sha(A) + " 3",
				"work/b.jpg " + sha(B) + " 4");
	}

	@Test
	void publishedBackupsHaveAManifestToo() throws IOException {
		backups().publish(site);

		assertThat(manifest(newest("published")).get("images")).hasSize(3);
	}

	@Test
	void archiveHoldsEveryManifestImageUnderItsHash() throws IOException {
		backups().backup(site);

		assertThat(archived()).containsExactlyInAnyOrder(sha(A) + ".jpg", sha(B) + ".jpg", sha(HOME) + ".jpg");
		assertThat(backupDir.resolve("images").resolve(sha(A) + ".jpg")).hasBinaryContent(A);
	}

	@Test
	void archiveFilesAreHardLinksToTheGallery() throws IOException {
		backups().backup(site);

		assertThat(Files.isSameFile(backupDir.resolve("images").resolve(sha(A) + ".jpg"), gallery.resolve("work/a.jpg"))).isTrue();
	}

	@Test
	void backingUpAnUnchangedSiteAddsNoFiles() throws IOException {
		JsonBackup backup = backups();
		backup.backup(site);
		List<String> before = archived();

		backup.backup(site);
		backup.publish(site);

		assertThat(archived()).isEqualTo(before);
	}

	@Test
	void identicalImagesAreArchivedOnce() throws IOException {
		Files.write(gallery.resolve("work/b.jpg"), A); // same bytes as a.jpg

		backups().backup(site);

		assertThat(archived()).containsExactlyInAnyOrder(sha(A) + ".jpg", sha(HOME) + ".jpg");
	}

	@Test
	void aMissingImageIsRecordedAndTheBackupStillWritten() throws IOException {
		Files.delete(gallery.resolve("work/b.jpg"));

		backups().backup(site);

		List<String> missing = new ArrayList<>();
		manifest(newest("unpublished")).get("images").forEach(i -> {
			if (i.path("missing").asBoolean()) missing.add(i.get("path").asText());
		});
		assertThat(missing).containsExactly("work/b.jpg");
	}

	@Test
	void manifestDoesNotChangeTheRestoredSite() {
		JsonBackup backup = backups();
		backup.backup(site);

		assertThat(backup.restore()).usingRecursiveComparison().isEqualTo(site);
	}

	@Test
	void backupsFromBeforeManifestsStillRestore() throws IOException {
		Files.createDirectories(backupDir);
		Files.writeString(backupDir.resolve("1-unpublished-12000020250101-site-data-backup.json"),
				mapper.writeValueAsString(site));

		assertThat(backups().restore().getName()).isEqualTo("Test Site");
	}

	// ---- BACKUP-3

	@Test
	void retiringAnImageRemovesItFromTheGalleryButKeepsItArchived() throws IOException {
		JsonBackup backup = backups();
		backup.backup(site);

		assertThat(backup.retireImage("work", "a.jpg")).isTrue();

		assertThat(gallery.resolve("work/a.jpg")).doesNotExist();
		assertThat(backupDir.resolve("images").resolve(sha(A) + ".jpg")).hasBinaryContent(A);
	}

	@Test
	void retiringArchivesFirstEvenWithoutAPriorBackup() throws IOException {
		JsonBackup backup = backups();

		assertThat(backup.retireImage("", "home.jpg")).isTrue();

		assertThat(backupDir.resolve("images").resolve(sha(HOME) + ".jpg")).hasBinaryContent(HOME);
	}

	@Test
	void retiringSomethingThatIsNotThereFails() {
		JsonBackup backup = backups();

		assertThat(backup.retireImage("work", "nope.jpg")).isFalse();
		assertThat(backup.retireImage("", "")).isFalse();
		assertThat(gallery).isDirectory();
	}

	@Test
	void aDeletedPaintingStaysRestorableUntilItsLastBackupRotatesOut() throws IOException {
		JsonBackup backup = backups(2, 3, false);
		backup.publish(site);           // published backups referencing a.jpg
		backup.publish(site);
		backup.backup(site);            // draft referencing a.jpg
		backup.retireImage("work", "a.jpg");
		site.getPage("work").removePainting("a.jpg");
		Path archivedA = backupDir.resolve("images").resolve(sha(A) + ".jpg");

		backup.backup(site);
		backup.backup(site);            // drafts: 2 without a, 1 with a; published still has a
		assertThat(archivedA).exists();

		backup.backup(site);            // last draft with a rotated out, but the published ones still need it
		assertThat(archivedA).exists();

		backup.publish(site);
		backup.publish(site);           // now nothing retained references a.jpg
		assertThat(archivedA).doesNotExist();
		assertThat(archived()).containsExactlyInAnyOrder(sha(B) + ".jpg", sha(HOME) + ".jpg");
	}

	@Test
	void liveImagesAreNeverCollected() throws IOException {
		JsonBackup backup = backups(1, 1, false);
		for (int i = 0; i < 5; i++) {
			backup.backup(site);
			backup.publish(site);
		}

		assertThat(gallery.resolve("work/a.jpg")).exists();
		assertThat(gallery.resolve("work/b.jpg")).exists();
		assertThat(gallery.resolve("home.jpg")).exists();
		assertThat(archived()).hasSize(3);
	}

	@Test
	void aDeletedPagesImagesGoOnceNoBackupNeedsThem() throws IOException {
		JsonBackup backup = backups(1, 2, false);
		backup.backup(site);
		backup.publish(site);
		site.removePage("work");          // like PageController: the folder is left behind

		backup.backup(site);
		assertThat(gallery.resolve("work/a.jpg")).exists(); // an older draft still lists it

		backup.backup(site);
		backup.publish(site);
		assertThat(gallery.resolve("work/a.jpg")).doesNotExist();
		assertThat(gallery.resolve("work/b.jpg")).doesNotExist();
		assertThat(gallery.resolve("home.jpg")).exists();
	}

	@Test
	void unreferencedFilesThatWereNeverArchivedAreLeftAlone() throws IOException {
		Path stray = Files.write(Files.createDirectories(gallery.resolve("wales")).resolve("old.jpg"), new byte[]{7});
		JsonBackup backup = backups(1, 1, false);

		for (int i = 0; i < 3; i++) backup.backup(site);

		assertThat(stray).exists();
	}

	@Test
	void dryRunDeletesNothing() throws IOException {
		JsonBackup backup = backups(1, 1, true);
		backup.backup(site);
		backup.publish(site);
		site.removePage("work");
		backup.retireImage("", "home.jpg"); // explicit retire still moves it into the archive
		site.setHomeImage(painting("", 0).toBuilder().filename("").build());

		for (int i = 0; i < 3; i++) {
			backup.backup(site);
			backup.publish(site);
		}

		assertThat(gallery.resolve("work/a.jpg")).exists();
		assertThat(gallery.resolve("work/b.jpg")).exists();
		assertThat(archived()).hasSize(3);
	}

	@Test
	void cleanupIsSkippedWhenARetainedBackupCantBeRead() throws IOException {
		JsonBackup backup = backups(2, 3, false);
		backup.publish(site);
		backup.retireImage("work", "a.jpg");
		site.getPage("work").removePainting("a.jpg");
		Files.writeString(newest("published"), "corrupt");

		for (int i = 0; i < 4; i++) backup.backup(site);

		assertThat(backupDir.resolve("images").resolve(sha(A) + ".jpg")).exists();
	}
}
