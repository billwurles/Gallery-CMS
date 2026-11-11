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
		// Milliseconds, so two saves in the same second still get different names (and so different version ids).
		// Older backups have HHmmssyyyyMMdd here; both are just digits, and only the file times are used for dates.
		String newFilename = buildFilename(1, published, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")));
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

	// ---------------------------------------------------------------- publish status

	/**
	 * Whether the site has changes the live site doesn't. {@code lastPublished} is when the newest published
	 * backup was written (null if never), {@code changedSince} when the newest draft was (only when changed).
	 */
	public record PublishStatus(boolean changed, Instant lastPublished, Instant changedSince) {}

	private record PublishedCopy(Path path, long modified, JsonNode content) {}
	private PublishedCopy publishedCache;

	/** Compares the site as it is now with the newest published backup (image manifests aside). */
	public synchronized PublishStatus status(Site site) {
		List<BackupFile> published = findBackups(true);
		if (published.isEmpty()) return new PublishStatus(true, null, newestDraftTime());
		BackupFile newest = published.get(0);
		try {
			long modified = Files.getLastModifiedTime(newest.path()).toMillis();
			if (publishedCache == null || !publishedCache.path().equals(newest.path()) || publishedCache.modified() != modified) {
				ObjectNode json = (ObjectNode) objectMapper.readTree(newest.path().toFile());
				json.remove(MANIFEST_FIELD);
				publishedCache = new PublishedCopy(newest.path(), modified, json);
			}
			boolean changed = !objectMapper.valueToTree(site).equals(publishedCache.content());
			return new PublishStatus(changed, Instant.ofEpochMilli(modified), changed ? newestDraftTime() : null);
		} catch (IOException | JacksonException | ClassCastException e) {
			log.warn("Can't read the newest published backup to compare: {}", e.getMessage());
			return new PublishStatus(true, null, newestDraftTime());
		}
	}

	private Instant newestDraftTime() {
		List<BackupFile> drafts = findBackups(false);
		if (drafts.isEmpty()) return null;
		try {
			return Files.getLastModifiedTime(drafts.get(0).path()).toInstant();
		} catch (IOException e) {
			return null;
		}
	}

	// ---------------------------------------------------------------- versions (Backups page)

	/**
	 * One kept backup. {@code missing} lists images it needs that can't be brought back: manifest entries the
	 * archive lacks, or, for a backup from before manifests, referenced files not in the gallery.
	 */
	public record Snapshot(String id, boolean published, Instant time, String siteName, int pages, int paintings,
						   int images, boolean hasManifest, List<String> missing) {}

	/** Every kept backup, newest first, drafts and published versions together. */
	public synchronized List<Snapshot> snapshots() {
		List<BackupFile> all = new ArrayList<>(findBackups(false));
		all.addAll(findBackups(true));
		List<Snapshot> out = new ArrayList<>();
		for (BackupFile file : all) {
			try {
				JsonNode json = objectMapper.readTree(file.path().toFile());
				int paintings = 0;
				for (JsonNode page : json.path("pages")) paintings += page.path("gallery").path("gallery").size();
				JsonNode manifest = json.path(MANIFEST_FIELD);
				out.add(new Snapshot(versionId(file.path()), file.published(),
						Files.getLastModifiedTime(file.path()).toInstant(), json.path("name").asText(""),
						json.path("pages").size(), paintings, referencedImages(json).size(),
						!manifest.isMissingNode(), missingImages(json)));
			} catch (IOException | JacksonException e) {
				log.warn("Can't read backup {} for the list: {}", file.path().getFileName(), e.getMessage());
			}
		}
		out.sort(Comparator.comparing(Snapshot::time).reversed());
		return out;
	}

	private List<String> missingImages(JsonNode json) {
		List<String> missing = new ArrayList<>();
		JsonNode manifest = json.path(MANIFEST_FIELD);
		if (manifest.isMissingNode()) {
			if (galleryRoot != null) {
				for (String path : referencedImages(json)) {
					if (!Files.isRegularFile(galleryRoot.resolve(path))) missing.add(path);
				}
			}
			return missing;
		}
		for (JsonNode image : manifest.path("images")) {
			String path = image.path("path").asText("");
			if (!image.hasNonNull("sha256") || !archive.contains(image.get("sha256").asText(), path)) missing.add(path);
		}
		return missing;
	}

	/**
	 * A version's id: its file name without the leading number. Saves renumber the files (1-, 2-, ...), so the
	 * number can't identify a version, but the kind, time and name stay the same as it moves down the list.
	 */
	public static String versionId(Path file) {
		return file.getFileName().toString().replaceFirst("^\\d+-", "");
	}

	/** The backup file for this version id, if it's one of the kept backups (nothing else can be named). */
	private Path snapshotFile(String id) {
		List<BackupFile> all = new ArrayList<>(findBackups(false));
		all.addAll(findBackups(true));
		return all.stream().map(BackupFile::path).filter(p -> versionId(p).equals(id)).findFirst()
				.orElseThrow(() -> new IllegalArgumentException("There's no kept version " + id));
	}

	/** The result of a restore: the site as it was, and any images that couldn't be brought back. */
	public record Restored(Site site, List<String> missing) {}

	/**
	 * Reads a kept backup and puts its images back into the gallery from the archive (hard link, else copy,
	 * checked against the hash). The caller saves the current site first, so the restore can be undone,
	 * and saves the restored one after.
	 */
	public synchronized Restored restoreSnapshot(String file) {
		Path path = snapshotFile(file);
		JsonNode json = objectMapper.readTree(path.toFile());
		List<String> missing = new ArrayList<>();
		JsonNode manifest = json.path(MANIFEST_FIELD);
		if (manifest.isMissingNode()) {
			missing.addAll(missingImages(json)); // from before manifests: the JSON only
		} else if (galleryRoot != null) {
			for (JsonNode image : manifest.path("images")) {
				String relative = image.path("path").asText("");
				if (!image.hasNonNull("sha256")) { missing.add(relative); continue; }
				try {
					if (!rehydrate(relative, image.get("sha256").asText())) missing.add(relative);
				} catch (IOException e) {
					log.error("Couldn't restore image {}", relative, e);
					missing.add(relative);
				}
			}
		}
		((ObjectNode) json).remove(MANIFEST_FIELD);
		Site site = objectMapper.treeToValue(json, Site.class);
		log.info("Restored {} ({} image(s) couldn't be brought back)", file, missing.size());
		return new Restored(site, missing);
	}

	/** Makes gallery/relative hold the archived image with this hash; false if the archive doesn't have it. */
	private boolean rehydrate(String relative, String sha) throws IOException {
		Path target = galleryRoot.resolve(relative).normalize();
		if (!target.startsWith(galleryRoot.normalize())) return false;
		if (Files.isRegularFile(target) && archive.sha256(target).equals(sha)) return true;
		Path source = archive.pathFor(sha, relative);
		if (!Files.isRegularFile(source) || !ImageArchive.hashOf(source).equals(sha)) return false;
		Files.createDirectories(target.getParent());
		Files.deleteIfExists(target);
		try {
			Files.createLink(target, source);
		} catch (IOException | UnsupportedOperationException e) {
			Files.copy(source, target);
		}
		return ImageArchive.hashOf(target).equals(sha);
	}

	/** A kept backup as a zip: site.json (with its manifest) and images/<gallery path>. */
	public synchronized void writeZip(String file, java.io.OutputStream out) throws IOException {
		Path path = snapshotFile(file);
		JsonNode json = objectMapper.readTree(path.toFile());
		try (java.util.zip.ZipOutputStream zip = new java.util.zip.ZipOutputStream(out)) {
			zip.putNextEntry(new java.util.zip.ZipEntry("site.json"));
			zip.write(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(json));
			zip.closeEntry();
			JsonNode manifest = json.path(MANIFEST_FIELD);
			Set<String> written = new HashSet<>();
			for (String relative : referencedImages(json)) {
				Path image = null;
				for (JsonNode entry : manifest.path("images")) {
					if (entry.path("path").asText("").equals(relative) && entry.hasNonNull("sha256")) {
						image = archive.pathFor(entry.get("sha256").asText(), relative);
					}
				}
				if ((image == null || !Files.isRegularFile(image)) && galleryRoot != null) image = galleryRoot.resolve(relative);
				if (image == null || !Files.isRegularFile(image) || !written.add(relative)) continue;
				zip.putNextEntry(new java.util.zip.ZipEntry("images/" + relative));
				Files.copy(image, zip);
				zip.closeEntry();
			}
		}
	}

	/**
	 * What's wrong with the images, if anything: archive files whose content no longer matches their hash,
	 * live paintings whose file is missing, and images a kept backup needs that the archive lacks.
	 */
	public record IntegrityReport(int archived, List<String> corrupt, List<String> missingFromGallery,
								  List<String> missingFromArchive) {
		public boolean ok() {
			return corrupt.isEmpty() && missingFromGallery.isEmpty() && missingFromArchive.isEmpty();
		}
	}

	public synchronized IntegrityReport checkIntegrity(Site site) {
		List<String> corrupt = new ArrayList<>();
		Set<String> archived = new TreeSet<>();
		try {
			archived.addAll(archive.list());
		} catch (IOException e) {
			log.error("Can't list the image archive", e);
		}
		for (String name : archived) {
			String expected = name.contains(".") ? name.substring(0, name.indexOf('.')) : name;
			try {
				if (!ImageArchive.hashOf(archive.getArchiveDir().resolve(name)).equals(expected)) corrupt.add(name);
			} catch (IOException e) {
				corrupt.add(name);
			}
		}
		List<String> missingFromGallery = new ArrayList<>();
		if (galleryRoot != null) {
			for (String path : referencedImages(objectMapper.valueToTree(site))) {
				if (!Files.isRegularFile(galleryRoot.resolve(path))) missingFromGallery.add(path);
			}
		}
		Set<String> missingFromArchive = new TreeSet<>();
		for (Snapshot snapshot : snapshots()) {
			if (snapshot.hasManifest()) snapshot.missing().forEach(m -> missingFromArchive.add(m + " (" + snapshot.id() + ")"));
		}
		return new IntegrityReport(archived.size(), corrupt, missingFromGallery, new ArrayList<>(missingFromArchive));
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
