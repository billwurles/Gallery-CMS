package es.burl.cms.helper;

import es.burl.cms.data.ImageUploadDTO;
import es.burl.cms.data.Page;
import es.burl.cms.data.Painting;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;

import static es.burl.cms.TestSites.page;
import static es.burl.cms.TestSites.painting;
import static org.assertj.core.api.Assertions.assertThat;

class FilesystemTest {

	@TempDir
	Path galleryRoot;

	private static final byte[] BYTES = {1, 2, 3, 4, 5};

	private static ImageUploadDTO.ImageData upload(String filename) {
		return new ImageUploadDTO.ImageData("My title", filename, "30x40cm", "Acrylic", true, 0,
				Base64.getEncoder().encodeToString(BYTES));
	}

	@Test
	void uploadPaintingWritesDecodedBytesUnderARandomName() throws IOException {
		Painting painting = Filesystem.uploadPainting(upload("photo.JPG"), 7, "work", galleryRoot);

		assertThat(painting.getFilename()).matches("[0-9a-f-]{36}\\.JPG");
		assertThat(Files.readAllBytes(galleryRoot.resolve("work").resolve(painting.getFilename()))).isEqualTo(BYTES);
		assertThat(painting.getTitle()).isEqualTo("My title");
		assertThat(painting.getDimensions()).isEqualTo("30x40cm");
		assertThat(painting.getMedium()).isEqualTo("Acrylic");
		assertThat(painting.isSold()).isTrue();
		assertThat(painting.getOrder()).isEqualTo(7);
	}

	@Test
	void uploadWithEmptyUrlGoesToGalleryRoot() {
		Painting painting = Filesystem.uploadPainting(upload("home.png"), 0, "", galleryRoot);

		assertThat(galleryRoot.resolve(painting.getFilename())).exists();
	}

	@Test
	void uploadNamesAreUnique() {
		Painting a = Filesystem.uploadPainting(upload("same.jpg"), 0, "work", galleryRoot);
		Painting b = Filesystem.uploadPainting(upload("same.jpg"), 1, "work", galleryRoot);

		assertThat(a.getFilename()).isNotEqualTo(b.getFilename());
	}

	@Test
	void uploadNewPaintingsAppendsAfterExistingOnes() {
		Page work = page("Work", "work", 0);
		work.addPaintingToGallery(painting("existing.jpg", 0));
		ImageUploadDTO dto = new ImageUploadDTO(List.of(upload("one.jpg"), upload("two.jpg")));

		assertThat(Filesystem.uploadNewPaintings(dto, work, galleryRoot)).isTrue();

		assertThat(work.getGallery().getGallery()).hasSize(3);
		assertThat(work.getGallery().getGalleryInOrder()).extracting(Painting::getOrder).containsExactly(0, 1, 2);
	}

	@Test
	void deletePaintingRemovesTheFile() throws IOException {
		Path image = Files.createDirectories(galleryRoot.resolve("work")).resolve("a.jpg");
		Files.write(image, BYTES);

		assertThat(Filesystem.deletePainting("a.jpg", "work", galleryRoot)).isTrue();
		assertThat(image).doesNotExist();
	}

	@Test
	void deletingAMissingPaintingReportsFalse() {
		assertThat(Filesystem.deletePainting("nope.jpg", "work", galleryRoot)).isFalse();
	}

	@Test
	void getImageServesTheFile() throws IOException {
		Files.write(Files.createDirectories(galleryRoot.resolve("work")).resolve("a.png"), BYTES);

		ResponseEntity<Resource> response = Filesystem.getImageFromPainting("a.png", "work", galleryRoot);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE)).isIn("image/png", "application/octet-stream");
		assertThat(response.getBody().getContentAsByteArray()).isEqualTo(BYTES);
	}

	@Test
	void getMissingImageIs404() {
		ResponseEntity<Resource> response = Filesystem.getImageFromPainting("missing.png", "work", galleryRoot);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void copyGalleryImagesToNewUrlCopiesEveryFile() throws IOException {
		Path old = Files.createDirectories(galleryRoot.resolve("old"));
		Files.write(old.resolve("a.jpg"), BYTES);
		Files.write(old.resolve("b.jpg"), BYTES);

		assertThat(Filesystem.copyGalleryImagesToNewUrl("old", "new", galleryRoot)).isTrue();

		assertThat(galleryRoot.resolve("new/a.jpg")).hasBinaryContent(BYTES);
		assertThat(galleryRoot.resolve("new/b.jpg")).hasBinaryContent(BYTES);
	}

	@Test
	void copyImageOverwrites() throws IOException {
		Path src = Files.write(galleryRoot.resolve("src.jpg"), BYTES);
		Path target = Files.createDirectories(galleryRoot.resolve("out"));
		Files.write(target.resolve("x.jpg"), new byte[]{9});

		Filesystem.copyImage("x.jpg", src.toFile(), target);

		assertThat(target.resolve("x.jpg")).hasBinaryContent(BYTES);
	}

	@Test
	void copyClassPathResourceCopiesBundledStatic() throws IOException {
		Filesystem.copyClassPathResource("style.css", "templates/site/static/", galleryRoot);

		assertThat(Files.size(galleryRoot.resolve("style.css"))).isPositive();
	}

	@Test
	void sortByAspectRatioOrdersPortraitToLandscape() throws IOException {
		Page work = galleryOfShapes();

		Filesystem.sortPaintingsByAspectRatio(work, galleryRoot, false);

		assertThat(work.getGallery().getGalleryInOrder()).extracting(Painting::getFilename)
				.containsExactly("tall.png", "square.png", "wide.png");
	}

	@Test
	void sortByAspectRatioReversed() throws IOException {
		Page work = galleryOfShapes();

		work.sortPaintingsByAspectRatio(galleryRoot, true);

		assertThat(work.getGallery().getGalleryInOrder()).extracting(Painting::getFilename)
				.containsExactly("wide.png", "square.png", "tall.png");
	}

	@Test
	void sortByAspectRatioKeepsMetadata() throws IOException {
		Page work = galleryOfShapes();

		Filesystem.sortPaintingsByAspectRatio(work, galleryRoot, false);

		assertThat(work.getGallery().getPainting("wide.png").getTitle()).isEqualTo("Title wide.png");
	}

	private Page galleryOfShapes() throws IOException {
		Page work = page("Work", "work", 0);
		Path dir = Files.createDirectories(galleryRoot.resolve("work"));
		writePng(dir.resolve("wide.png"), 40, 10);
		writePng(dir.resolve("square.png"), 10, 10);
		writePng(dir.resolve("tall.png"), 10, 40);
		work.addPaintingToGallery(painting("wide.png", 0));
		work.addPaintingToGallery(painting("square.png", 1));
		work.addPaintingToGallery(painting("tall.png", 2));
		return work;
	}

	static void writePng(Path path, int width, int height) throws IOException {
		ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), "png", path.toFile());
	}
}
