package es.burl.cms.editor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.burl.cms.data.Gallery;
import es.burl.cms.data.Page;
import es.burl.cms.data.Painting;
import es.burl.cms.data.Site;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RequestMapping("/page/{pageUrl}/gallery")
@Controller
public class GalleryController {

	private final Site site;
	private final String galleryRoot;

	@Autowired
	public GalleryController(Site site, @Qualifier("galleryRoot") String galleryRoot) {
		this.site = site;
		this.galleryRoot = galleryRoot;
	}

	@GetMapping(value = { "", "/" })
	public String editGallery(@PathVariable("pageUrl") String pageUrl, Model model){
		model.addAttribute("menuItems", site.getMenuItems());
		Page page = site.getPage(pageUrl);
		log.debug("Editing gallery for page: {}", page.toString());

		if (page != null) {
			model.addAttribute("message", "Editing gallery "+page.getTitle());
			model.addAttribute("page", page);  // Pass the page to the model
			model.addAttribute("galleryRoot", galleryRoot);
			model.addAttribute("hasGallery", page.getGallery() != null);
			return "editor/EditGallery";
		} else {
			return "404";  // Return a "404.html" page if page not found
		}
	}

	@PostMapping("/save")
	public String saveGallery(@PathVariable("pageUrl") String pageUrl,
							  @RequestBody Gallery gallery,
							  Model model) {
		// Get the page object
		Page page = site.getPage(pageUrl);
//		if (page != null && gallery != null) {
//			List<Painting> updatedGallery = new ArrayList<>();
//
//			// Update the gallery with the new list of paintings
//			page.setGallery(gallery);
//		}

		// Save the updated page with the new gallery order
		site.addNewPage(page.getTitle(), pageUrl, page.getContent(), page.isShowInMenu(), gallery);

		// Pass the saved content back to the view
		model.addAttribute("message", "Content saved successfully!");
		model.addAttribute("page", page);
		model.addAttribute("hasGallery", page.getGallery() != null);
		model.addAttribute("menuItems", site.getMenuItems());

		return "editor/EditGallery";
	}



	@GetMapping("/upload")
	public String getUploadPage(@PathVariable("pageUrl") String pageUrl, Model model) {
		Page page = site.getPage(pageUrl);
		if (page != null) {
			model.addAttribute("menuItems", site.getMenuItems());
			model.addAttribute("page", page);
			model.addAttribute("message", "Upload and Edit Metadata for " + page.getTitle());
			return "editor/UploadGallery";
		} else {
			return "404"; // Return a 404 page if the page is not found
		}
	}

	@PostMapping("/upload")
	public String uploadGallery(@PathVariable("pageUrl") String pageUrl,
								@RequestParam("images") List<MultipartFile> images,
								@RequestParam("metadata") String metadataJson,
								Model model) throws JsonProcessingException {
		Page page = site.getPage(pageUrl);
		if (page == null) {
			return "404"; // Return a 404 page if the page is not found
		}

		// Parse the metadata JSON into Painting objects
		ObjectMapper objectMapper = new ObjectMapper();
		List<Painting> paintings = objectMapper.readValue(metadataJson,
				objectMapper.getTypeFactory().constructCollectionType(List.class, Painting.class));

		// Check if the number of images matches the number of metadata entries
		if (paintings.size() != images.size()) {
			log.warn("Mismatch between images and metadata: {} images, {} metadata entries", images.size(), paintings.size());
			// TODO: send this message to frontend
			// Optionally, send this message to the frontend
			// Return error page or message if necessary
		}

		// Save images to the filesystem and update painting objects with the image path
		for (int i = 0; i < images.size(); i++) {
			MultipartFile image = images.get(i);
			Painting painting = paintings.get(i); // Get the current painting metadata

			if (!image.isEmpty()) {
				String filename = painting.getFilename();  // Use the filename from the request
				Path uploadPath = Paths.get(galleryRoot, pageUrl, filename);
				try {
					Files.createDirectories(uploadPath.getParent());
					Files.write(uploadPath, image.getBytes());

					// Save updated gallery to the site object
					page.addPaintingToGallery(painting);
				} catch (IOException e) {
					log.error("Failed to save image: {}", filename, e);
				}
			} else {
				log.warn("Skipped empty file: {}", image.getOriginalFilename());
			}
		}

		model.addAttribute("menuItems", site.getMenuItems());
		model.addAttribute("page", page);
		model.addAttribute("message", "Gallery uploaded and metadata saved successfully!");

		return "editor/EditGallery";
	}

	@GetMapping("/image/{filename}")
	public ResponseEntity<Resource> getImage(
			@PathVariable String pageUrl,
			@PathVariable String filename) {
		log.debug("getting image {}",filename);
		try {
			// Construct the path to the image
			Path imagePath = Paths.get(galleryRoot, pageUrl, filename);
			Resource resource = new UrlResource(imagePath.toUri());

			if (resource.exists() && resource.isReadable()) {
				// Return the image as a downloadable resource
				return ResponseEntity.ok()
						.header(HttpHeaders.CONTENT_TYPE, "image/jpeg") // Adjust for other types if needed
						.body(resource);
			} else {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(null);
			}
		} catch (MalformedURLException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(null);
		}
	}
}
