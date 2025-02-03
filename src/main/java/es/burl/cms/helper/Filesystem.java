package es.burl.cms.helper;

import es.burl.cms.data.ImageUploadDTO;
import es.burl.cms.data.Page;
import es.burl.cms.data.Painting;
import lombok.experimental.Helper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.*;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.Objects;

@Slf4j
public class Filesystem {

	//TODO: Move all my filesystem crap into here

	public static boolean uploadNewPaintings(ImageUploadDTO imageUploadDTO, Page page, Path galleryRoot){
		log.debug("Got imageDTO: {}", imageUploadDTO.toString());

		// Iterate over the images and metadata
		int order = page.getGallery().getGallery().size();
		for (ImageUploadDTO.ImageData newImage : imageUploadDTO.getImages()) {
			page.addPaintingToGallery(uploadPainting(newImage, order++, page.getUrl(), galleryRoot));
		}
		return true;
	}

	public static Painting uploadPainting(ImageUploadDTO.ImageData newImage, int order, String url, Path galleryRoot) {
		String imageData = newImage.getImageData();
		byte[] imageBytes = Base64.getDecoder().decode(imageData); // Decode base64 image

		String filename = Painting.generateFilename(newImage.getFilename());
		// Construct the file path and save the image

		Path uploadPath = (url.equals("") ? galleryRoot : galleryRoot.resolve(url)).resolve(filename);
		try {
			Files.createDirectories(uploadPath.getParent());
			Files.write(uploadPath, imageBytes);  // Save the image file

			// Create a Painting object and update gallery
			Painting painting = Painting.builder().title(newImage.getTitle()).filename(filename).dimensions(newImage.getDimensions()).medium(newImage.getMedium()).sold(newImage.isSold()).order(order).build();

			log.debug("Creating new Painting object: {}", painting);
			return painting;
		} catch (IOException e) {
			log.error("Failed to save image: {}", newImage.getFilename(), e);
			return null;
		}
	}

	public static boolean deletePainting(String filename, String url, Path galleryRoot){
		log.debug("Deleting painting {}", filename);
		Path imagePath = galleryRoot.resolve(url).resolve(filename);
		File file = imagePath.toFile();
		if(file.exists())
			return file.delete();
		else return false;
	}

	public static ResponseEntity<Resource> getImageFromPainting(String filename, String url, Path galleryRoot) {
		try {
			log.debug("trying to get image {}", filename);
			// Construct the path to the image
			Path imagePath = galleryRoot.resolve(url).resolve(filename);
			String mimeType = Files.probeContentType(imagePath);
			Resource resource = new UrlResource(imagePath.toUri());

			if (resource.exists() && resource.isReadable()) {
				log.debug("Got image {} - {}",filename, resource.contentLength());
				// Return the image as a downloadable resource
				return ResponseEntity.ok()
						.header(HttpHeaders.CONTENT_TYPE, mimeType != null ? mimeType : "application/octet-stream")
						.body(resource);
			} else {
				log.error("Could not find image {} in directory - {}", url+"/"+filename, imagePath.toString());
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
			}
		} catch (MalformedURLException e) {
			log.error("Error constructing URI path {}", url+"/"+filename);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
		} catch (IOException e) {
			log.error("Error detecting MIME type {}", url+"/"+filename);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
		}
	}

	public static boolean copyGalleryImagesToNewUrl(String oldPageUrl, String newPageUrl, Path galleryRoot) {
		// Construct the page path and image directory path, create dirs if they don't exist
		Path newPageGallery = galleryRoot.resolve(newPageUrl);
		File oldPageGallery = galleryRoot.resolve(oldPageUrl).toFile();
		try {
			Files.createDirectories(newPageGallery);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		log.debug("Moving gallery folder from {} to {}",oldPageUrl, newPageUrl);

		//Copy all the images from a gallery
		for(File file : Objects.requireNonNull(oldPageGallery.listFiles())) {
			String filename = file.getName();
			try {
				copyImage(filename, file, newPageGallery);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		return true; //TODO: add more edges cases and return false
	}

	public static void copyImage(String filename, File file, Path imageDir) throws IOException {
		log.debug("Copying image {}", filename);
		Path imagePath = imageDir.resolve(filename);
		Files.copy(file.toPath(), imagePath, StandardCopyOption.REPLACE_EXISTING);
	}

	public static void copyClassPathResource(String filename, String fromDirectory, Path toDirectory) throws IOException {
		ClassPathResource resource = new ClassPathResource(fromDirectory+filename);
		InputStream inputStream = resource.getInputStream();
		File outputFile = toDirectory.resolve(filename).toFile();

		OutputStream outputStream = new FileOutputStream(outputFile);
		byte[] buffer = new byte[8192];
		int length;
		while ((length = inputStream.read(buffer)) > 0) {
			outputStream.write(buffer, 0, length);
		}
	}
}
