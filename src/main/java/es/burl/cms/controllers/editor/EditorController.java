package es.burl.cms.controllers.editor;

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
	private final Path backupPath;
	private final Path galleryRoot;
	private final ThymeleafSiteBuilder siteBuilder;

	@Autowired
	public EditorController(
			Site site,
			@Qualifier("getBackupPath") Path backupPath,
			@Qualifier("getGalleryRoot") Path galleryRoot,
			ThymeleafSiteBuilder siteBuilder
	) {
		this.site = site;
		this.backupPath = backupPath;
		this.galleryRoot = galleryRoot;
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
	@GetMapping("save") //TODO: Give some feedback about success to the user
	public ResponseEntity<?> saveSiteToJSON(Model model) {
		try {
			new JsonBackup(backupPath).backup(site);
			siteBuilder.buildSite(site);

		} catch (RuntimeException e) {
			ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred saving site");
		} catch (IOException e) {
			ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred building site");
		}
		return ResponseEntity.ok("Site saved");
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
			Filesystem.deletePainting(site.getHomeImage().getFilename(), "", galleryRoot);
			site.setHomeImage(homeImage);
			return ResponseEntity.ok("Image uploaded successfully");
		}

		System.err.println("Error saving home image");
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred during image upload");
	}

	@PostMapping("/rearrange")
	public String rearrangePages(@RequestBody Map<Integer, String> order) {
		site.updatePageOrder(order);
		return "redirect:/"; // Redirect to the home after saving
	}
}
