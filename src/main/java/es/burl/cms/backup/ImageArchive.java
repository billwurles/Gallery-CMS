package es.burl.cms.backup;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Content-addressed store of every image a backup needs: {@code <archiveDir>/<sha256><.ext>}.
 * <p>
 * Gallery images are never modified in place (each upload gets a new random name), so a hard link from the
 * gallery costs no extra disk and stays valid after the gallery file is deleted. Falls back to a copy when
 * linking isn't possible.
 */
@Slf4j
public class ImageArchive {

	private final Path archiveDir;

	/** sha256 by (absolute path, size, mtime), so repeated backups don't re-read unchanged photos. */
	private final Map<CacheKey, String> hashCache = new ConcurrentHashMap<>();

	private record CacheKey(Path path, long size, long modified) {}

	public ImageArchive(Path archiveDir) {
		this.archiveDir = archiveDir;
	}

	public Path getArchiveDir() {
		return archiveDir;
	}

	public String sha256(Path file) throws IOException {
		BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
		CacheKey key = new CacheKey(file.toAbsolutePath().normalize(), attrs.size(), attrs.lastModifiedTime().toMillis());
		String cached = hashCache.get(key);
		if (cached != null) return cached;
		String hash = hashOf(file);
		hashCache.put(key, hash);
		return hash;
	}

	static String hashOf(Path file) throws IOException {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			try (InputStream in = new DigestInputStream(Files.newInputStream(file), digest)) {
				in.transferTo(java.io.OutputStream.nullOutputStream());
			}
			return HexFormat.of().formatHex(digest.digest());
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	/** The archive file name for an image: its hash plus the source's lower-cased extension. */
	public static String archiveName(String sha256, String sourceName) {
		int dot = sourceName.lastIndexOf('.');
		String ext = dot >= 0 ? sourceName.substring(dot).toLowerCase(Locale.ROOT) : "";
		return sha256 + ext;
	}

	public Path pathFor(String sha256, String sourceName) {
		return archiveDir.resolve(archiveName(sha256, sourceName));
	}

	public boolean contains(String sha256, String sourceName) {
		return Files.isRegularFile(pathFor(sha256, sourceName));
	}

	/**
	 * Makes sure the archive holds the content of {@code file}, and returns its hash.
	 * Safe to call repeatedly: an image that is already archived costs a cached hash lookup.
	 */
	public String archive(Path file) throws IOException {
		String sha = sha256(file);
		Path target = pathFor(sha, file.getFileName().toString());
		if (Files.isRegularFile(target)) return sha;

		Files.createDirectories(archiveDir);
		try {
			Files.createLink(target, file);
			log.debug("Archived {} as {} (hard link)", file, target.getFileName());
		} catch (FileAlreadyExistsException e) {
			return sha;
		} catch (IOException | UnsupportedOperationException e) {
			// Not Files.createTempFile, which makes an owner-only file: archived images keep normal permissions
			Path tmp = archiveDir.resolve(".tmp-" + UUID.randomUUID() + ".part");
			try {
				Files.copy(file, tmp, StandardCopyOption.REPLACE_EXISTING);
				if (!hashOf(tmp).equals(sha)) throw new IOException("Archive copy of " + file + " doesn't match its hash");
				Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE);
				log.debug("Archived {} as {} (copy)", file, target.getFileName());
			} finally {
				Files.deleteIfExists(tmp);
			}
		}
		return sha;
	}

	/** Every archived file name (hash + extension), ignoring temp files. */
	public Set<String> list() throws IOException {
		if (!Files.isDirectory(archiveDir)) return Set.of();
		try (Stream<Path> files = Files.list(archiveDir)) {
			Set<String> names = new TreeSet<>();
			files.filter(Files::isRegularFile)
					.map(p -> p.getFileName().toString())
					.filter(n -> !n.startsWith("."))
					.forEach(names::add);
			return names;
		}
	}

}
