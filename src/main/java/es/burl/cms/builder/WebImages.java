package es.burl.cms.builder;

import lombok.extern.slf4j.Slf4j;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
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
 * Web-sized copies of gallery images for the published site: no bigger than a given longest side, never
 * enlarged, and turned the right way up. Phone photos are often stored sideways with an EXIF orientation
 * tag, and re-encoding drops that tag, so the rotation is applied to the pixels.
 * <p>
 * Resized copies are kept in a cache folder, named by the source's content hash and the size, so a publish
 * only resizes images it hasn't seen. An image already within the size is used as it is (its own EXIF tag
 * still applies in the browser). Anything that can't be decoded is published untouched.
 */
@Slf4j
public class WebImages {

	private static final float JPEG_QUALITY = 0.85f;

	private final Path cacheDir;
	private final Map<CacheKey, String> hashes = new ConcurrentHashMap<>();
	private final Set<String> usedThisBuild = ConcurrentHashMap.newKeySet();

	private record CacheKey(Path path, long size, long modified) {}

	public WebImages(Path cacheDir) {
		this.cacheDir = cacheDir;
	}

	/** Start of a publish: forget which cache entries were used. */
	public void startBuild() {
		usedThisBuild.clear();
	}

	/**
	 * After a successful publish: delete cached copies that build didn't use (images since removed or
	 * replaced). Returns how many were deleted.
	 */
	public int pruneUnused() throws IOException {
		if (!Files.isDirectory(cacheDir)) return 0;
		int removed = 0;
		try (Stream<Path> files = Files.list(cacheDir)) {
			for (Path file : files.toList()) {
				if (!usedThisBuild.contains(file.getFileName().toString())) {
					Files.deleteIfExists(file);
					removed++;
				}
			}
		}
		if (removed > 0) log.debug("Removed {} unused web image(s) from the cache", removed);
		return removed;
	}

	/** Puts a copy of {@code source} no bigger than {@code maxSide} at {@code target}: a hard link where possible. */
	public void publish(Path source, int maxSide, Path target) throws IOException {
		Path sized = sized(source, maxSide);
		Files.createDirectories(target.getParent());
		Files.deleteIfExists(target);
		try {
			Files.createLink(target, sized);
		} catch (IOException | UnsupportedOperationException e) {
			Files.copy(sized, target, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	/** The file to publish for {@code source} at {@code maxSide}: the source itself, or a cached resized copy. */
	Path sized(Path source, int maxSide) throws IOException {
		String ext = extension(source);
		if (!ext.equals(".jpg") && !ext.equals(".jpeg") && !ext.equals(".png")) return source;

		int[] size = dimensions(source);
		if (size == null) {
			log.warn("Can't read the size of {}; publishing it as it is", source);
			return source;
		}
		if (Math.max(size[0], size[1]) <= maxSide) return source;

		String name = hash(source).substring(0, 20) + "-" + maxSide + (ext.equals(".png") ? ".png" : ".jpg");
		Path cached = cacheDir.resolve(name);
		usedThisBuild.add(name);
		if (Files.isRegularFile(cached)) return cached;

		BufferedImage image;
		try {
			image = ImageIO.read(source.toFile());
		} catch (IOException | RuntimeException e) {
			image = null;
		}
		if (image == null) {
			log.warn("Can't decode {}; publishing it as it is", source);
			usedThisBuild.remove(name);
			return source;
		}
		if (!ext.equals(".png")) image = orient(image, exifOrientation(source));
		BufferedImage resized = resize(image, maxSide, ext.equals(".png"));

		Files.createDirectories(cacheDir);
		Path tmp = cacheDir.resolve(".tmp-" + UUID.randomUUID() + ".part");
		try {
			write(resized, ext.equals(".png"), tmp);
			Files.move(tmp, cached, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
		} finally {
			Files.deleteIfExists(tmp);
		}
		log.debug("Resized {} ({}x{}) to {}x{} for the web", source.getFileName(), size[0], size[1], resized.getWidth(), resized.getHeight());
		return cached;
	}

	/** Width and height without decoding the pixels, or null if unreadable. */
	public static int[] dimensions(Path file) {
		try (ImageInputStream in = ImageIO.createImageInputStream(file.toFile())) {
			if (in == null) return null;
			Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
			if (!readers.hasNext()) return null;
			ImageReader reader = readers.next();
			try {
				reader.setInput(in, true, true);
				return new int[]{reader.getWidth(0), reader.getHeight(0)};
			} finally {
				reader.dispose();
			}
		} catch (IOException | RuntimeException e) {
			return null;
		}
	}

	/** Scales so the longest side is {@code maxSide}, halving first for large reductions so it stays smooth. */
	static BufferedImage resize(BufferedImage image, int maxSide, boolean keepAlpha) {
		int w = image.getWidth(), h = image.getHeight();
		double scale = Math.min(1.0, (double) maxSide / Math.max(w, h));
		int targetW = Math.max(1, (int) Math.round(w * scale)), targetH = Math.max(1, (int) Math.round(h * scale));
		int type = keepAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;

		BufferedImage current = image;
		do {
			int nextW = Math.max(targetW, current.getWidth() / 2), nextH = Math.max(targetH, current.getHeight() / 2);
			if (current.getWidth() / 2 < targetW || current.getHeight() / 2 < targetH) {
				nextW = targetW;
				nextH = targetH;
			}
			BufferedImage next = new BufferedImage(nextW, nextH, type);
			Graphics2D g = next.createGraphics();
			if (!keepAlpha) {
				g.setColor(Color.WHITE); // transparent pixels (rare in a JPEG source) become white, not black
				g.fillRect(0, 0, nextW, nextH);
			}
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g.drawImage(current, 0, 0, nextW, nextH, null);
			g.dispose();
			current = next;
		} while (current.getWidth() != targetW || current.getHeight() != targetH);
		return current;
	}

	private static void write(BufferedImage image, boolean png, Path target) throws IOException {
		if (png) {
			if (!ImageIO.write(image, "png", target.toFile())) throw new IOException("No PNG writer");
			return;
		}
		ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
		try (ImageOutputStream out = ImageIO.createImageOutputStream(target.toFile())) {
			writer.setOutput(out);
			ImageWriteParam param = writer.getDefaultWriteParam();
			param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			param.setCompressionQuality(JPEG_QUALITY);
			param.setProgressiveMode(ImageWriteParam.MODE_DEFAULT);
			writer.write(null, new IIOImage(image, null, null), param);
		} finally {
			writer.dispose();
		}
	}

	/**
	 * Applies an EXIF orientation (1-8) to the pixels. For each displayed pixel (x, y) it reads the stored
	 * pixel the EXIF definition names: 2 mirrored, 3 upside down, 4 mirrored vertically, 5 transposed,
	 * 6 turned 90 degrees clockwise, 7 transverse, 8 turned 90 degrees anticlockwise.
	 */
	static BufferedImage orient(BufferedImage image, int orientation) {
		if (orientation < 2 || orientation > 8) return image;
		int w = image.getWidth(), h = image.getHeight();
		boolean swap = orientation >= 5;
		int outW = swap ? h : w, outH = swap ? w : h;
		int[] src = image.getRGB(0, 0, w, h, null, 0, w);
		int[] dst = new int[outW * outH];
		for (int y = 0; y < outH; y++) {
			for (int x = 0; x < outW; x++) {
				int sx, sy;
				switch (orientation) {
					case 2 -> { sx = w - 1 - x; sy = y; }
					case 3 -> { sx = w - 1 - x; sy = h - 1 - y; }
					case 4 -> { sx = x; sy = h - 1 - y; }
					case 5 -> { sx = y; sy = x; }
					case 6 -> { sx = y; sy = h - 1 - x; }
					case 7 -> { sx = w - 1 - y; sy = h - 1 - x; }
					default -> { sx = w - 1 - y; sy = x; } // 8
				}
				dst[y * outW + x] = src[sy * w + sx];
			}
		}
		BufferedImage out = new BufferedImage(outW, outH, BufferedImage.TYPE_INT_RGB);
		out.setRGB(0, 0, outW, outH, dst, 0, outW);
		return out;
	}

	/** The EXIF orientation tag of a JPEG (1 = upright), read straight from its APP1 segment. */
	static int exifOrientation(Path jpeg) {
		byte[] b;
		try (InputStream in = Files.newInputStream(jpeg)) {
			b = in.readNBytes(256 * 1024); // the EXIF block is near the start
		} catch (IOException e) {
			return 1;
		}
		if (b.length < 4 || (b[0] & 0xFF) != 0xFF || (b[1] & 0xFF) != 0xD8) return 1;
		int i = 2;
		while (i + 4 <= b.length) {
			if ((b[i] & 0xFF) != 0xFF) return 1;
			int marker = b[i + 1] & 0xFF;
			if (marker == 0xDA || marker == 0xD9) return 1; // image data starts: no EXIF before it
			int length = ((b[i + 2] & 0xFF) << 8) | (b[i + 3] & 0xFF);
			int start = i + 4;
			if (marker == 0xE1 && start + 6 <= b.length && new String(b, start, 4, java.nio.charset.StandardCharsets.ISO_8859_1).equals("Exif")) {
				return tiffOrientation(b, start + 6, Math.min(b.length, i + 2 + length));
			}
			i += 2 + length;
		}
		return 1;
	}

	private static int tiffOrientation(byte[] b, int tiff, int end) {
		if (tiff + 8 > end) return 1;
		boolean little = b[tiff] == 'I';
		int ifd = tiff + readInt(b, tiff + 4, little);
		if (ifd + 2 > end) return 1;
		int count = readShort(b, ifd, little);
		for (int e = 0; e < count; e++) {
			int entry = ifd + 2 + e * 12;
			if (entry + 12 > end) return 1;
			if (readShort(b, entry, little) == 0x0112) {
				int value = readShort(b, entry + 8, little);
				return value >= 1 && value <= 8 ? value : 1;
			}
		}
		return 1;
	}

	private static int readShort(byte[] b, int at, boolean little) {
		return little ? (b[at] & 0xFF) | ((b[at + 1] & 0xFF) << 8) : ((b[at] & 0xFF) << 8) | (b[at + 1] & 0xFF);
	}

	private static int readInt(byte[] b, int at, boolean little) {
		return little
				? (b[at] & 0xFF) | ((b[at + 1] & 0xFF) << 8) | ((b[at + 2] & 0xFF) << 16) | ((b[at + 3] & 0xFF) << 24)
				: ((b[at] & 0xFF) << 24) | ((b[at + 1] & 0xFF) << 16) | ((b[at + 2] & 0xFF) << 8) | (b[at + 3] & 0xFF);
	}

	private String hash(Path file) throws IOException {
		BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
		CacheKey key = new CacheKey(file.toAbsolutePath().normalize(), attrs.size(), attrs.lastModifiedTime().toMillis());
		String known = hashes.get(key);
		if (known != null) return known;
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			try (InputStream in = new DigestInputStream(Files.newInputStream(file), digest)) {
				in.transferTo(java.io.OutputStream.nullOutputStream());
			}
			String hash = HexFormat.of().formatHex(digest.digest());
			hashes.put(key, hash);
			return hash;
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	private static String extension(Path file) {
		String name = file.getFileName().toString();
		int dot = name.lastIndexOf('.');
		return dot < 0 ? "" : name.substring(dot).toLowerCase(Locale.ROOT);
	}
}
