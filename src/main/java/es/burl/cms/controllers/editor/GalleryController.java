package es.burl.cms.controllers.editor;

import es.burl.cms.backup.BackupSite;
import es.burl.cms.builder.ThymeleafSiteBuilder;
import es.burl.cms.builder.WebImages;
import es.burl.cms.data.*;
import es.burl.cms.helper.Filesystem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RequestMapping("/page/{pageUrl}/gallery")
@Controller
public class GalleryController {

	private final Site site;
	private final Path galleryRoot;
	private final BackupSite saveService;
	private final ThymeleafSiteBuilder builder;

	/** For tests that don't need thumbnails (the original image is served instead). */
	public GalleryController(Site site, Path galleryRoot, BackupSite saveService) {
		this(site, galleryRoot, saveService, null);
	}

	@Autowired
	public GalleryController(@Qualifier("getSite") Site site,
							 @Qualifier("getGalleryRoot") Path galleryRoot,
							 @Qualifier("getSaveService") BackupSite saveService,
							 ThymeleafSiteBuilder builder) {
		this.site = site;
		this.galleryRoot = galleryRoot;
		this.saveService = saveService;
		this.builder = builder;
	}

	@GetMapping(value = {"", "/"})
	public String editGallery(@PathVariable("pageUrl") String pageUrl, Model model) {
		model.addAttribute("menuItems", site.getMenuItems());
		Page page = site.getPage(pageUrl);
		if (page == null) throw new NotFoundException("The page " + pageUrl);
		log.debug("Editing gallery for page: {}", pageUrl);

		model.addAttribute("message", "Editing gallery " + page.getMenuItem().getTitle());
		model.addAttribute("page", page);
		model.addAttribute("galleryRoot", galleryRoot);
		model.addAttribute("hasGallery", page.hasGallery());
		model.addAttribute("fileSizes", fileSizes(page));
		return "editor/EditGallery";
	}

	//TODO: allow boolean flags per gallery of whether or not to display title etc info

	@PostMapping(value = {"", "/"})
	public ResponseEntity<Map<String, String>> saveGallery(@PathVariable("pageUrl") String pageUrl, @RequestBody Gallery gallery) {
		Page page = site.getPage(pageUrl);

		Map<String, String> response = new HashMap<>();
		if (page == null) {
			response.put("status", "failure");
			response.put("message", "The page " + pageUrl + " was not found");
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
		}

		if(page.saveGallery(gallery, galleryRoot)){
			// Success message
			response.put("status", "success");
			response.put("message", "Content saved successfully!");
			saveService.backup(site);
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
		if (page == null) throw new NotFoundException("The page " + pageUrl);
		model.addAttribute("menuItems", site.getMenuItems());
		model.addAttribute("page", page.getUrl());
		model.addAttribute("message", "Upload and Edit Metadata for " + page.getMenuItem().getTitle());
		return "editor/UploadGallery";
	}

	//TODO: pass in nextInt in order
	//TODO: check unique filenames
	@PostMapping(value = "/upload", consumes = MediaType.APPLICATION_JSON_VALUE)
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
			saveService.backup(site);
			return ResponseEntity.ok("Gallery uploaded successfully");
		} else {
			System.err.println("Error uploading gallery");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred during gallery upload");
		}
	}

	/**
	 * One image, uploaded as a normal file (multipart), added to the end of the gallery. The upload page
	 * sends one request per file, so each has its own progress and its own success or error.
	 */
	@PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<String> uploadFile(@PathVariable("pageUrl") String pageUrl,
											 @RequestParam("file") MultipartFile file,
											 @RequestParam(defaultValue = "") String title,
											 @RequestParam(defaultValue = "") String dimensions,
											 @RequestParam(defaultValue = "") String medium,
											 @RequestParam(defaultValue = "false") boolean sold) throws java.io.IOException {
		synchronized (site) { // files arrive in parallel; each takes the next place in the gallery
			Page page = site.getPage(pageUrl);
			if (page == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("That page was not found");
			if (page.getGallery() == null) page.setGallery(Gallery.builder().build());
			int order = page.getGallery().getGallery().values().stream().mapToInt(Painting::getOrder).max().orElse(-1) + 1;
			String name = title.isBlank() ? Filesystem.nameWithoutExtension(file.getOriginalFilename()) : title;
			Painting painting = Filesystem.savePainting(file.getBytes(), file.getOriginalFilename(), name, dimensions, medium, sold,
					order, page.getUrl(), galleryRoot);
			if (painting == null) {
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("\"" + file.getOriginalFilename() + "\" couldn't be saved on the server");
			}
			page.addPaintingToGallery(painting);
			saveService.backup(site);
			return ResponseEntity.ok(painting.getFilename());
		}
	}

	@GetMapping("/image/{filename}")
	public ResponseEntity<Resource> getImage(@PathVariable String pageUrl, @PathVariable String filename,
											 @RequestParam(name = "size", required = false) String size) {
		// Only images of paintings on an existing page: never arbitrary files from the gallery folder
		Page page = site.getPage(pageUrl);
		if (page == null || page.getGallery() == null || page.getGallery().getPainting(filename) == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}
		if ("thumb".equals(size) && builder != null) {
			// The editor's grid shows the small cached copy, not the multi-MB original
			try {
				Path thumb = builder.thumbnail(galleryRoot.resolve(page.getUrl()).resolve(filename));
				return ResponseEntity.ok()
						.contentType(MediaTypeFactory.getMediaType(thumb.getFileName().toString()).orElse(MediaType.IMAGE_JPEG))
						.body(new FileSystemResource(thumb));
			} catch (java.io.IOException e) {
				log.warn("No thumbnail for {}: {}", filename, e.getMessage());
			}
		}
		return Filesystem.getImageFromPainting(filename, pageUrl, galleryRoot);
	}

	@DeleteMapping("/image/{filename}")
	public  ResponseEntity<?> deletePainting(@PathVariable String pageUrl, @PathVariable String filename) {
		log.debug("Deleting painting {}",filename);
		Page page = site.getPage(pageUrl);
		if (page == null || page.getGallery() == null || page.getGallery().getPainting(filename) == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("That painting was not found");
		}
		if(saveService.retireImage(pageUrl, filename)){ // file leaves the gallery but stays in the backup archive
			page.removePainting(filename);
			saveService.backup(site);
			return ResponseEntity.ok("Painting deleted successfully");
		}  else {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred during painting delete");

		}
	}

	/** The original image's size, for each card: the grid shows a smaller copy, so the browser can't tell. */
	public record ImageInfo(long bytes, int width, int height) {}

	/** Each painting's original file size and pixel size, for the editor to flag overly large uploads. */
	private Map<String, ImageInfo> fileSizes(Page page) {
		Map<String, ImageInfo> sizes = new HashMap<>();
		if (page.getGallery() == null) return sizes;
		for (Painting painting : page.getGallery().getGallery().values()) {
			Path file = galleryRoot.resolve(page.getUrl()).resolve(painting.getFilename());
			long bytes;
			try {
				bytes = java.nio.file.Files.size(file);
			} catch (java.io.IOException e) {
				bytes = 0;
			}
			int[] wh = WebImages.dimensions(file);
			sizes.put(painting.getFilename(), new ImageInfo(bytes, wh == null ? 0 : wh[0], wh == null ? 0 : wh[1]));
		}
		return sizes;
	}
}
