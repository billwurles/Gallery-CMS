package es.burl.cms.controllers.editor;

import com.fasterxml.jackson.core.JsonProcessingException;
import es.burl.cms.data.*;
import es.burl.cms.helper.Filesystem;
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

import java.io.File;
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
							 @Qualifier("getGalleryRoot") Path galleryRoot) {
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
			return "404";  // Return a "error.html" page if page not found
		}
	}

	//TODO: allow boolean flags per gallery of whether or not to display title etc info

	@PostMapping(value = {"", "/"})
	public ResponseEntity<Map<String, String>> saveGallery(@PathVariable("pageUrl") String pageUrl, @RequestBody Gallery gallery) {
		Page page = site.getPage(pageUrl);

		Map<String, String> response = new HashMap<>();

		if(page.saveGallery(gallery, galleryRoot)){
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

	@GetMapping("/upload")
	public String getUploadPage(@PathVariable("pageUrl") String pageUrl, Model model) {
		Page page = site.getPage(pageUrl);
		if (page != null) {
			model.addAttribute("menuItems", site.getMenuItems());
			model.addAttribute("page", page.getUrl());
			model.addAttribute("message", "Upload and Edit Metadata for " + page.getMenuItem().getTitle());
			return "editor/UploadGallery";
		} else {
			return "404"; // Return a 404 page if the page is not found
		}
	}

	//TODO: pass in nextInt in order
	//TODO: check unique filenames
	@PostMapping("/upload")
	public ResponseEntity<?> uploadGallery(@PathVariable("pageUrl") String pageUrl, @RequestBody ImageUploadDTO imageUploadDTO, Model model){
		Page page = site.getPage(pageUrl);
		if (page == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("That page was not found");
		}
		if (page.getGallery() == null) page.setGallery(Gallery.builder().build());
		boolean ok = Filesystem.uploadNewPaintings(imageUploadDTO, page, galleryRoot);

		model.addAttribute("menuItems", site.getMenuItems());
		model.addAttribute("page", page);
		if(ok) {
			return ResponseEntity.ok("Gallery uploaded successfully");
		} else {
			System.err.println("Error uploading gallery");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred during gallery upload");
		}
	}

	@GetMapping("/image/{filename}")
	public ResponseEntity<Resource> getImage(@PathVariable String pageUrl, @PathVariable String filename) {
		return Filesystem.getImageFromPainting(filename, pageUrl, galleryRoot);
	}

	@DeleteMapping("/image/{filename}")
	public  ResponseEntity<?> deletePainting(@PathVariable String pageUrl, @PathVariable String filename) {
		log.debug("Deleting painting {}",filename);
		Page page = site.getPage(pageUrl);
		if(Filesystem.deletePainting(filename, pageUrl, galleryRoot)){
			page.removePainting(filename); //TODO: delete from filesystem
			return ResponseEntity.ok("Painting deleted successfully");
		}  else {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred during painting delete");

		}
	}
}
