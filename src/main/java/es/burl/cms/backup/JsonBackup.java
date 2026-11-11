package es.burl.cms.backup;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import es.burl.cms.data.Site;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Numbered JSON snapshots of the site ({@code 1-unpublished-<time>-<name>.json} is the newest draft,
 * {@code 1-published-...} the newest publish), each carrying a manifest of the images it needs.
 * Those images are kept in an {@link ImageArchive} under {@code <backupPath>/images/} for as long as any
 * retained snapshot (or the live site) references them, so restoring an older snapshot never loses photos.
 */
@Slf4j
public class JsonBackup implements BackupSite {

	public static final String MANIFEST_FIELD = "backupManifest";
	public static final int MANIFEST_SCHEMA = 1;

	private final Path backupPath;
	private final String filename;
	private final int numPublishedBackups;
	private final int numUnpublishedBackups;
	private final Path galleryRoot;
	private final boolean gcDryRun;
	private final ImageArchive archive;
	private final Pattern backupName;

	private final static String publishedExt = "-published";
	private final static String unpublishedExt = "-unpublished";
	private final static String fileExt = ".json";

	/**
	 * Backups are read leniently (fields from newer or older versions are ignored) and dates stay as
	 * millisecond numbers, as every existing backup has them (Jackson 3 would otherwise write ISO text).
	 */
	private final ObjectMapper objectMapper = JsonMapper.builder()
			.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
			.enable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
			.build();

	/** Backups without image versioning (no gallery to archive). */
	public JsonBackup(Path backupPath, String filename, int numPublishedBackups, int numUnpublishedBackups) {
		this(backupPath, filename, numPublishedBackups, numUnpublishedBackups, null, true);
	}

	public JsonBackup(Path backupPath, String filename, int numPublishedBackups, int numUnpublishedBackups,
					  Path galleryRoot, boolean gcDryRun) {
		this.backupPath = backupPath;
		this.filename = filename;
		this.numPublishedBackups = numPublishedBackups;
		this.numUnpublishedBackups = numUnpublishedBackups;
		this.galleryRoot = galleryRoot;
		this.gcDryRun = gcDryRun;
		this.archive = new ImageArchive(backupPath.resolve("images"));
		// <n>-<published|unpublished>-<time>-<filename>.json, nothing else
		this.backupName = Pattern.compile("^(\\d+)-(published|unpublished)-(\\d+)-" + Pattern.quote(filename) + Pattern.quote(fileExt) + "$");
	}

	public ImageArchive getArchive() {
		return archive;
	}

	@Override
	public synchronized void backup(Site site) {
		save(site, false);
	}

	public synchronized void publish(Site site) {
		save(site, true);
	}

	private void save(Site site, boolean published) {
		ObjectNode json = objectMapper.valueToTree(site);
		if (galleryRoot != null) {
			json.set(MANIFEST_FIELD, archiveImages(json)); // archive first: a JSON never names an image the archive lacks
		}
		moveOldBackups(published);
		saveToJson(site, json, published);
		if (galleryRoot != null) {
			collectGarbage(json);
		}
	}

	// ---------------------------------------------------------------- rotation

	private record BackupFile(Path path, int number, boolean published, String time) {}

	/** Backup files of one kind, newest (lowest number) first. Ignores anything that isn't a backup. */
	private List<BackupFile> findBackups(boolean published) {
		List<BackupFile> found = new ArrayList<>();
		if (!Files.isDirectory(backupPath)) return found;
		try (Stream<Path> files = Files.list(backupPath)) {
			files.filter(Files::isRegularFile).forEach(path -> {
				Matcher m = backupName.matcher(path.getFileName().toString());
				if (m.matches() && m.group(2).equals(published ? "published" : "unpublished")) {
					found.add(new BackupFile(path, Integer.parseInt(m.group(1)), published, m.group(3)));
				}
			});
		} catch (IOException e) {
			log.error("Can't list backups in {}", backupPath, e);
		}
		found.sort(Comparator.comparingInt(BackupFile::number));
		return found;
	}

	private void moveOldBackups(boolean published) {
		int keep = published ? numPublishedBackups : numUnpublishedBackups;
		List<BackupFile> found = findBackups(published);
		// Oldest first, so renames never overwrite a file that hasn't moved yet
		for (int i = found.size() - 1; i >= 0; i--) {
			BackupFile file = found.get(i);
			try {
				if (file.number() >= keep) {
					log.debug("Deleting old backup {}", file.path().getFileName());
					Files.delete(file.path());
				} else {
					Path newName = backupPath.resolve(buildFilename(file.number() + 1, published, file.time()));
					log.debug("Moving backup {} to {}", file.path().getFileName(), newName.getFileName());
					Files.move(file.path(), newName, StandardCopyOption.REPLACE_EXISTING);
				}
			} catch (IOException e) {
				log.error("Failed to rotate backup {}", file.path(), e);
			}
		}
	}

	private void saveToJson(Site site, JsonNode json, boolean published) {
		String newFilename = buildFilename(1, published, LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmssyyyyMMdd")));
		log.debug("Backing up to JSON - {}", newFilename);
		try {
			Files.createDirectories(backupPath);
			// Write then rename, so a crash mid-write never leaves a broken "newest" backup.
			// Not Files.createTempFile: that makes the file owner-only, and backups must stay readable
			// to whatever copies them off the machine. Saves are synchronized, so the name can't clash.
			Path tmp = backupPath.resolve(".tmp-" + newFilename);
			try {
				Files.writeString(tmp, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json));
				Files.move(tmp, backupPath.resolve(newFilename), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			} finally {
				Files.deleteIfExists(tmp);
			}
		} catch (IOException | JacksonException e) { // Jackson 3 errors are unchecked
			log.error("Failed to backup site: {}", site.getName(), e);
			throw new RuntimeException(e);
		}
	}

	// ---------------------------------------------------------------- restore

	public Site buildSiteFromPath(Path path) {
		try {
			return objectMapper.readValue(Files.readString(path), Site.class);
		} catch (IOException | JacksonException e) { // Jackson 3 errors are unchecked
			log.error("Failed to read site from backup {}", path, e);
			throw new RuntimeException(e);
		}
	}

	/** The newest readable backup of one kind, skipping unreadable files; null if there is none. */
	public Site getJsonFromBackup(boolean published) {
		for (BackupFile file : findBackups(published)) {
			try {
				return buildSiteFromPath(file.path());
			} catch (RuntimeException e) {
				log.warn("Skipping unreadable backup {}", file.path().getFileName());
			}
		}
		return null;
	}

	/** The newest readable draft, else the newest readable publish, else null. */
	@Override
	public Site restore() {
		log.debug("Getting newest draft backup");
		Site site = getJsonFromBackup(false);
		if (site == null) {
			log.debug("No readable draft backup, getting newest published backup");
			site = getJsonFromBackup(true);
		}
		return site;
	}

	// ---------------------------------------------------------------- images

	/** Gallery-relative paths of every image a serialised site references (home image + paintings). */
	static Set<String> referencedImages(JsonNode site) {
		Set<String> paths = new LinkedHashSet<>();
		String home = site.path("homeImage").path("filename").asText("");
		if (!home.isBlank()) paths.add(home);
		site.path("pages").properties().forEach(entry -> {
			String url = entry.getValue().path("menuItem").path("url").asText(entry.getKey());
			entry.getValue().path("gallery").path("gallery").forEach(painting -> {
				String file = painting.path("filename").asText("");
				if (!file.isBlank()) paths.add(url.isEmpty() ? file : url + "/" + file);
			});
		});
		return paths;
	}

	private ObjectNode archiveImages(JsonNode site) {
		ObjectNode manifest = objectMapper.createObjectNode();
		manifest.put("schemaVersion", MANIFEST_SCHEMA);
		manifest.put("created", Instant.now().toString());
		ArrayNode images = manifest.putArray("images");
		for (String relative : referencedImages(site)) {
			ObjectNode entry = images.addObject();
			entry.put("path", relative);
			Path file = galleryRoot.resolve(relative);
			try {
				if (!Files.isRegularFile(file)) {
					log.warn("Backup references a missing image: {}", relative);
					entry.put("missing", true);
					continue;
				}
				entry.put("sha256", archive.archive(file));
				entry.put("size", Files.size(file));
			} catch (IOException e) {
				log.error("Failed to archive image {}", relative, e);
				entry.put("missing", true);
			}
		}
		return manifest;
	}

	/**
	 * Removes a gallery image only once its content is safely in the archive, so older backups can still
	 * restore it. Returns false if there's no such image or it couldn't be archived (and leaves it in place).
	 */
	@Override
	public synchronized boolean retireImage(String pageUrl, String imageFilename) {
		if (galleryRoot == null || imageFilename == null || imageFilename.isBlank()) return false;
		Path file = (pageUrl == null || pageUrl.isEmpty() ? galleryRoot : galleryRoot.resolve(pageUrl)).resolve(imageFilename);
		if (!Files.isRegularFile(file)) return false;
		try {
			archive.archive(file);
			Files.delete(file);
			log.debug("Retired image {} into the archive", galleryRoot.relativize(file));
			return true;
		} catch (IOException e) {
			log.error("Failed to retire image {}", file, e);
			return false;
		}
	}

	// ---------------------------------------------------------------- garbage collection

	/**
	 * After rotation: drop gallery files that neither the live site nor any retained backup references, once
	 * their content has been archived, then archived images that no retained backup's manifest needs.
	 * Unreferenced gallery files that were never archived (e.g. left over from before image versioning) are
	 * only logged, never deleted.
	 */
	private void collectGarbage(JsonNode live) {
		Set<String> neededArchive = new HashSet<>();
		Set<String> neededGallery = new HashSet<>(referencedImages(live));
		boolean allReadable = true;
		for (boolean published : new boolean[]{false, true}) {
			for (BackupFile file : findBackups(published)) {
				try {
					JsonNode json = objectMapper.readTree(file.path().toFile());
					neededGallery.addAll(referencedImages(json));
					json.path(MANIFEST_FIELD).path("images").forEach(image -> {
						if (image.hasNonNull("sha256")) {
							neededArchive.add(ImageArchive.archiveName(image.get("sha256").asText(), image.path("path").asText("")));
						}
					});
				} catch (JacksonException e) {
					log.warn("Can't read backup {} during cleanup; skipping cleanup this time", file.path().getFileName());
					allReadable = false;
				}
			}
		}
		if (!allReadable) return; // never delete on partial information

		try {
			Set<String> archived = archive.list();
			// Gallery first: an unreferenced file can go once its content has been archived at some point
			if (Files.isDirectory(galleryRoot)) {
				int neverArchived = 0;
				try (Stream<Path> files = Files.walk(galleryRoot)) {
					for (Path file : files.filter(Files::isRegularFile).toList()) {
						String relative = galleryRoot.relativize(file).toString().replace('\\', '/');
						if (neededGallery.contains(relative)) continue;
						if (archived.contains(ImageArchive.archiveName(archive.sha256(file), file.getFileName().toString()))) {
							remove(file, "gallery image nothing references (its content is archived)");
						} else {
							log.debug("Unreferenced gallery file {} was never archived; leaving it in place", relative);
							neverArchived++;
						}
					}
				}
				if (neverArchived > 0) {
					log.info("{} gallery files aren't used by the site or any backup and were never archived; left in place", neverArchived);
				}
			}
			for (String name : archived) {
				if (!neededArchive.contains(name)) {
					remove(archive.getArchiveDir().resolve(name), "archived image no retained backup needs");
				}
			}
		} catch (IOException e) {
			log.error("Backup cleanup failed", e);
		}
	}

	private void remove(Path file, String why) throws IOException {
		if (gcDryRun) {
			log.info("[dry run] Would delete {}: {}", file, why);
		} else {
			log.info("Deleting {}: {}", file, why);
			Files.deleteIfExists(file);
		}
	}

	private String buildFilename(int num, boolean published, String dateTime) {
		return num + (published ? publishedExt : unpublishedExt) + "-" + dateTime + "-" + filename + fileExt;
	}
}
