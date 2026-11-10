package es.burl.cms.builder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

import static org.assertj.core.api.Assertions.assertThat;

class WebImagesTest {

	@TempDir
	Path tmp;

	Path cache;
	WebImages images;

	@BeforeEach
	void setUp() {
		cache = tmp.resolve("web-images");
		images = new WebImages(cache);
		images.startBuild();
	}

	@Test
	void largeImagesAreScaledDownToTheLongestSide() throws IOException {
		Path src = jpeg(tmp.resolve("wide.jpg"), 3000, 1500, 0);

		images.publish(src, 900, tmp.resolve("out/thumb.jpg"));
		images.publish(src, 2000, tmp.resolve("out/large.jpg"));

		assertThat(size(tmp.resolve("out/thumb.jpg"))).containsExactly(900, 450);
		assertThat(size(tmp.resolve("out/large.jpg"))).containsExactly(2000, 1000);
	}

	@Test
	void smallImagesArePublishedAsTheyAreAndNeverEnlarged() throws IOException {
		Path src = jpeg(tmp.resolve("small.jpg"), 800, 400, 0);

		images.publish(src, 900, tmp.resolve("out/thumb.jpg"));

		assertThat(tmp.resolve("out/thumb.jpg")).hasSameBinaryContentAs(src);
		assertThat(fileKey(tmp.resolve("out/thumb.jpg"))).isEqualTo(fileKey(src)); // a hard link, no extra disk
		assertThat(cache).doesNotExist();
	}

	@ParameterizedTest
	@CsvSource({"1, 900, 450", "3, 900, 450", "6, 450, 900", "8, 450, 900"})
	void phonePhotosAreTurnedUpright(int orientation, int width, int height) throws IOException {
		// Stored 3000x1500 with a red top-left corner; the EXIF tag says how to turn it for display
		Path src = jpeg(tmp.resolve("photo.jpg"), 3000, 1500, orientation);

		images.publish(src, 900, tmp.resolve("out.jpg"));

		assertThat(size(tmp.resolve("out.jpg"))).containsExactly(width, height);
	}

	@Test
	void everyExifOrientationPutsTheRightCornerTopLeft() {
		// 4x2 image: stored top-left red, top-right green, bottom-left blue, bottom-right white
		BufferedImage stored = new BufferedImage(4, 2, BufferedImage.TYPE_INT_RGB);
		for (int x = 0; x < 4; x++) {
			for (int y = 0; y < 2; y++) {
				int rgb = y == 0 ? (x < 2 ? 0xFF0000 : 0x00FF00) : (x < 2 ? 0x0000FF : 0xFFFFFF);
				stored.setRGB(x, y, rgb);
			}
		}
		// The stored corner that should be shown top-left for each orientation (EXIF 2.3, tag 0x0112)
		int[] topLeft = {0, 0xFF0000, 0x00FF00, 0xFFFFFF, 0x0000FF, 0xFF0000, 0x0000FF, 0xFFFFFF, 0x00FF00};
		for (int o = 1; o <= 8; o++) {
			BufferedImage shown = WebImages.orient(stored, o);
			assertThat(shown.getRGB(0, 0) & 0xFFFFFF).as("orientation %d", o).isEqualTo(topLeft[o]);
			assertThat(shown.getWidth()).as("orientation %d width", o).isEqualTo(o >= 5 ? 2 : 4);
		}
	}

	@Test
	void theOrientationTagIsReadFromTheFile() throws IOException {
		assertThat(WebImages.exifOrientation(jpeg(tmp.resolve("a.jpg"), 10, 10, 6))).isEqualTo(6);
		assertThat(WebImages.exifOrientation(jpeg(tmp.resolve("b.jpg"), 10, 10, 0))).isEqualTo(1);
	}

	@Test
	void pngsStayPngs() throws IOException {
		BufferedImage img = new BufferedImage(2000, 1000, BufferedImage.TYPE_INT_ARGB);
		Path src = tmp.resolve("logo.png");
		ImageIO.write(img, "png", src.toFile());

		images.publish(src, 900, tmp.resolve("out.png"));

		assertThat(ImageIO.read(tmp.resolve("out.png").toFile()).getColorModel().hasAlpha()).isTrue();
		assertThat(size(tmp.resolve("out.png"))).containsExactly(900, 450);
	}

	@Test
	void resizedCopiesAreCachedAndUnusedOnesPruned() throws IOException {
		Path a = jpeg(tmp.resolve("a.jpg"), 3000, 1500, 0);
		Path b = jpeg(tmp.resolve("b.jpg"), 1500, 3000, 0);
		images.publish(a, 900, tmp.resolve("out/a.jpg"));
		images.publish(b, 900, tmp.resolve("out/b.jpg"));
		Path cachedA = images.sized(a, 900);
		var made = Files.getLastModifiedTime(cachedA);

		// next publish: a is still used, b has gone
		images.startBuild();
		images.publish(a, 900, tmp.resolve("out2/a.jpg"));
		assertThat(images.pruneUnused()).isEqualTo(1);

		assertThat(Files.getLastModifiedTime(cachedA)).isEqualTo(made); // reused, not resized again
		try (var files = Files.list(cache)) {
			assertThat(files.toList()).containsExactly(cachedA);
		}
	}

	@Test
	void unreadableImagesArePublishedUntouched() throws IOException {
		Path src = Files.write(tmp.resolve("broken.jpg"), new byte[]{1, 2, 3});

		images.publish(src, 900, tmp.resolve("out.jpg"));

		assertThat(tmp.resolve("out.jpg")).hasBinaryContent(new byte[]{1, 2, 3});
	}

	/** A JPEG of the given stored size, with an EXIF orientation tag unless orientation is 0. */
	static Path jpeg(Path path, int width, int height, int orientation) throws IOException {
		BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		g.setColor(Color.GRAY);
		g.fillRect(0, 0, width, height);
		g.setColor(Color.RED);
		g.fillRect(0, 0, Math.max(1, width / 4), Math.max(1, height / 4));
		g.dispose();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write(img, "jpg", out);
		byte[] jpg = out.toByteArray();
		if (orientation == 0) return Files.write(path, jpg);

		// APP1 "Exif\0\0" + big-endian TIFF header + IFD0 holding one entry: Orientation (0x0112), SHORT
		byte[] tiff = {'M', 'M', 0, 42, 0, 0, 0, 8, 0, 1, 0x01, 0x12, 0, 3, 0, 0, 0, 1, 0, (byte) orientation, 0, 0, 0, 0, 0, 0};
		byte[] exif = new byte[6 + tiff.length];
		System.arraycopy("Exif".getBytes(), 0, exif, 0, 4);
		System.arraycopy(tiff, 0, exif, 6, tiff.length);
		int len = exif.length + 2;
		ByteArrayOutputStream withExif = new ByteArrayOutputStream();
		withExif.write(jpg, 0, 2); // SOI
		withExif.write(new byte[]{(byte) 0xFF, (byte) 0xE1, (byte) (len >> 8), (byte) len});
		withExif.write(exif);
		withExif.write(jpg, 2, jpg.length - 2);
		return Files.write(path, withExif.toByteArray());
	}

	static int[] size(Path image) throws IOException {
		BufferedImage img = ImageIO.read(image.toFile());
		return new int[]{img.getWidth(), img.getHeight()};
	}

	static Object fileKey(Path file) throws IOException {
		return Files.readAttributes(file, BasicFileAttributes.class).fileKey();
	}
}
