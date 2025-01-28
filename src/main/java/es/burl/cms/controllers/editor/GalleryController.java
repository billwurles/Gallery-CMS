package es.burl.cms.controllers.editor;

import com.fasterxml.jackson.core.JsonProcessingException;
import es.burl.cms.data.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RequestMapping("/page/{pageUrl}/gallery")
@Controller
public class GalleryController {

	private final Site site;
	private final Path galleryRoot;

	@Autowired
	public GalleryController(Site site,
							 @Qualifier("getGalleryRoot") Path galleryRoot
	) {
		this.site = site;
		this.galleryRoot = galleryRoot;
	}

	@GetMapping(value = {"", "/"})
	public String editGallery(@PathVariable("pageUrl") String pageUrl, Model model) {
		model.addAttribute("menuItems", site.getMenuItems());
		Page page = site.getPage(pageUrl);
		log.debug("Editing gallery for page: {}", page.toString());

		if (page != null) {
			model.addAttribute("message", "Editing gallery " + page.getMenuItem().getTitle());
			model.addAttribute("page", page);  // Pass the page to the model
			model.addAttribute("galleryRoot", galleryRoot);
			model.addAttribute("hasGallery", page.getGallery() != null);
			return "editor/EditGallery";
		} else {
			return "404";  // Return a "404.html" page if page not found
		}
	}

	@PostMapping("/save")
	public ResponseEntity<Map<String, String>> saveGallery(@PathVariable("pageUrl") String pageUrl, @RequestBody Gallery gallery) {
		// Get the page object
		Page page = site.getPage(pageUrl);

		// Prepare a response map
		Map<String, String> response = new HashMap<>();

		if (page != null && gallery != null) {
			// Replace the old gallery with the new one
			page.setGallery(gallery);

			// Save the updated page with the new gallery

			site.addNewPage(Page.builder()
					.fromPage(page)
					.gallery(gallery)
					.build()
			);

			// Success message
			response.put("status", "success");
			response.put("message", "Content saved successfully!");
			return ResponseEntity.ok(response);
		} else {
			// Failure message
			response.put("status", "failure");
			response.put("message", "Error: Page or gallery data is missing.");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		}
	}

	//TODO: Be able to delete images from gallery / delete entire gallery

	@GetMapping("/upload")
	public String getUploadPage(@PathVariable("pageUrl") String pageUrl, Model model) {
		Page page = site.getPage(pageUrl);
		if (page != null) {
			model.addAttribute("menuItems", site.getMenuItems());
			model.addAttribute("page", page);
			model.addAttribute("message", "Upload and Edit Metadata for " + page.getMenuItem().getTitle());
			return "editor/UploadGallery";
		} else {
			return "404"; // Return a 404 page if the page is not found
		}
	}

	//TODO: pass in nextInt in order
	@PostMapping("/upload")
	public String uploadGallery(@PathVariable("pageUrl") String pageUrl, @RequestBody ImageUploadDTO imageUploadDTO, // Handle JSON payload
								Model model) throws JsonProcessingException {
		Page page = site.getPage(pageUrl);
		if (page == null) {
			return "404"; // Return a 404 page if the page is not found
		}

		log.debug("Got imageDTO: {}", imageUploadDTO.toString());

		// Iterate over the images and metadata
		int order = page.getGallery().getGallery().size();
		for (ImageUploadDTO.ImageData newImage : imageUploadDTO.getImages()) {
			String imageData = newImage.getImageData();
			byte[] imageBytes = Base64.getDecoder().decode(imageData); // Decode base64 image

			// Construct the file path and save the image
			Path uploadPath = galleryRoot.resolve(pageUrl).resolve(newImage.getFilename());
			try {
				Files.createDirectories(uploadPath.getParent());
				Files.write(uploadPath, imageBytes);  // Save the image file

				// Create a Painting object and update gallery
				Painting painting = Painting.builder()
						.title(newImage.getTitle())
						.filename(newImage.getFilename())
						.dimensions(newImage.getDimensions())
						.medium(newImage.getMedium())
						.sold(newImage.isSold())
						.order(order++)
						.build();

				log.debug("Creating new Painting object: {}", painting);
				page.addPaintingToGallery(painting);

			} catch (IOException e) {
				log.error("Failed to save image: {}", newImage.getFilename(), e);
			}
		}

		model.addAttribute("menuItems", site.getMenuItems());
		model.addAttribute("page", page);
		model.addAttribute("message", "Gallery uploaded and metadata saved successfully!");

		//		return "redirect: /page/"+pageUrl+"/gallery";
		return "editor/EditGallery"; //TODO: automatic redirect back to gallery
	}

	@GetMapping("/image/{filename}")
	public ResponseEntity<Resource> getImage(@PathVariable String pageUrl, @PathVariable String filename) {
//		log.debug("getting image {}", filename);
		try {
			// Construct the path to the image
			Path imagePath = galleryRoot.resolve(pageUrl).resolve(filename);
			Resource resource = new UrlResource(imagePath.toUri());

			if (resource.exists() && resource.isReadable()) {
				// Return the image as a downloadable resource
				return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, "image/jpeg") // Adjust for other types if needed
						.body(resource);
			} else {
				log.error("Could not find image {} in directory - {}", pageUrl+"/"+filename, imagePath.toString());
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
			}
		} catch (MalformedURLException e) {
			log.error("Error constructing URI path {}", pageUrl+"/"+filename);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
		}
	}
}
