package es.burl.cms.controllers.editor;

import es.burl.cms.backup.BackupSite;
import es.burl.cms.backup.JsonBackup;
import es.burl.cms.builder.ThymeleafSiteBuilder;
import es.burl.cms.data.*;
import es.burl.cms.helper.Filesystem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
public class EditorController {

	private final Site site;
	private final Path galleryRoot;
	private final ThymeleafSiteBuilder siteBuilder;
	private final BackupSite saveService;
	private final JsonBackup publishService;

	@Autowired
	public EditorController(
			@Qualifier("getSite") Site site,
			@Qualifier("getGalleryRoot") Path galleryRoot,
			@Qualifier("getSaveService") BackupSite saveService,
			@Qualifier("getPublishService") JsonBackup publishService,
			ThymeleafSiteBuilder siteBuilder
	) {
		this.site = site;
		this.galleryRoot = galleryRoot;
		this.saveService = saveService;
		this.publishService = publishService;
		this.siteBuilder = siteBuilder;
	}


	@GetMapping("/")
	public String getHomePage(Model model) {
		model.addAttribute("page", "home");
		model.addAttribute("name", site.getName());
		model.addAttribute("menuItems", site.getMenuItems());
		model.addAttribute("homeImage", site.getHomeImage());
//		model.addAttribute("pages", site.getPagesInOrder());
		return "editor/EditorHome";
	}

	//TODO: BUG: When editing a painting filename, the filename is immediately changed, but if no save occurs a later restart forgets that change - save filename changes until save?
	/**
	 * Publishes the site: builds it (swapped in only if the whole build succeeds), then records the published
	 * backup. Errors come back as a 500 with a message the editor shows.
	 */
	@GetMapping("save")
	public ResponseEntity<String> saveSiteToJSON() {
		try {
			siteBuilder.buildSite(site);
		} catch (IOException | RuntimeException e) {
			log.error("Publishing failed while building the site", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("The site could not be built, so the live site is unchanged (" + describe(e) + ")");
		}
		try {
			publishService.publish(site);
		} catch (RuntimeException e) {
			log.error("The site was published but its backup failed", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("The site is live, but saving its published backup failed (" + describe(e) + ")");
		}
		return ResponseEntity.ok("Site published");
	}

	private static String describe(Throwable e) {
		Throwable root = e;
		while (root.getCause() != null && root.getCause() != root) root = root.getCause();
		String message = root.getMessage();
		return root.getClass().getSimpleName() + (message == null || message.isBlank() ? "" : ": " + message);
	}

	@GetMapping("/image/{filename}")
	public ResponseEntity<Resource> getHomeImage(@PathVariable String filename) {
		return Filesystem.getImageFromPainting(filename, "", galleryRoot);
	}

	@GetMapping("/upload")
	public String getUploadPage(Model model) {
		model.addAttribute("menuItems", site.getMenuItems());
		model.addAttribute("page", "");
		model.addAttribute("message", "Upload featured image for Home Page, only first Image is used"); //TODO: make it so you can ONLY upload one
		return "editor/UploadGallery";
	}

	@PostMapping("/upload")
	public ResponseEntity<?> uploadGallery(@RequestBody ImageUploadDTO imageData, Model model) {
		Painting homeImage = Filesystem.uploadPainting(imageData.getImages().get(0), 0, "", galleryRoot);

		if (homeImage != null) {
			saveService.retireImage("", site.getHomeImage().getFilename()); // kept in the backup archive
			site.setHomeImage(homeImage);
			saveService.backup(site);
			return ResponseEntity.ok("Image uploaded successfully");
		}

		System.err.println("Error saving home image");
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred during image upload");
	}

	@PostMapping("/rearrange")
	public ResponseEntity<?> rearrangePages(@RequestBody Map<Integer, String> order) {
		site.updatePageOrder(order);
		saveService.backup(site);
		return ResponseEntity.ok("Successful menu rearrange");
	}
}
