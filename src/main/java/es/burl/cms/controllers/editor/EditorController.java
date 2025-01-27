package es.burl.cms.controllers.editor;

import es.burl.cms.backup.JsonBackup;
import es.burl.cms.builder.ThymeleafSiteBuilder;
import es.burl.cms.data.Exhibition;
import es.burl.cms.data.Site;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
	private final ThymeleafSiteBuilder siteBuilder;

	@Autowired
	public EditorController(
			Site site,
			@Qualifier("getBackupPath") Path backupPath,
			ThymeleafSiteBuilder siteBuilder
	) {
		this.site = site;
		this.backupPath = backupPath;
		this.siteBuilder = siteBuilder;
	}


	@GetMapping("/")
	public String getHomePage(Model model) {
		model.addAttribute("page", "home");
		model.addAttribute("name", site.getName());
		model.addAttribute("menuItems", site.getMenuItems());
		model.addAttribute("pages", site.getPagesInOrder());
		return "editor/EditorHome";
	}

	@GetMapping("save")
	public String saveSiteToJSON(Model model) {
		try {
			new JsonBackup(backupPath).backup(site);

			siteBuilder.buildSite(site);

			model.addAttribute("message", "Site saved successfully");
		} catch (RuntimeException e) {
			model.addAttribute("message", "Error saving site");
		} catch (IOException e) {
			model.addAttribute("message", "Error building site");
			throw new RuntimeException(e);
		}
		model.addAttribute("page", "home");
		model.addAttribute("name", site.getName());
		model.addAttribute("menuItems", site.getMenuItems());
		model.addAttribute("pages", site.getPagesInOrder());
		return "editor/EditorHome";

	}

	@PostMapping("/rearrange")
	public String rearrangePages(@RequestBody Map<Integer, String> order) {
		site.updatePageOrder(order);

		return "redirect:/"; // Redirect to the home after saving
	}

	@GetMapping("/exhibitions")
	public String getExhibitions(
			@RequestParam(name = "page", defaultValue = "1") int page, // Query parameter "page"
			Model model) {
		int postsPerPage = 5;
		int totalPages = (int) Math.ceil((double) site.getExhibitionRepo().getExhibitions().size() / postsPerPage);
		List<Exhibition> exhibitions = site.getExhibitionRepo().getExhibitionsPage(page, postsPerPage);

		model.addAttribute("exhibitions", exhibitions);
		model.addAttribute("page", page);
		model.addAttribute("totalPages", totalPages);
		model.addAttribute("menuItems", site.getMenuItems());

		return "editor/ViewExhibitions";
	}

	@GetMapping("/exhibitions/{id}")
	public String editExhibition(@PathVariable("id") String id, Model model){
		model.addAttribute("name", site.getName());
		model.addAttribute("menuItems", site.getMenuItems());
		model.addAttribute("exhibition", site.getExhibitionRepo().get(id));
		return "editor/EditExhibition";
	}

	// Save method to handle POST requests
	@PostMapping("/exhibitions/{id}/save")
	public ResponseEntity<?> saveExhibition(@PathVariable("id") String id, @RequestBody Exhibition exhibition) {
		try {
			// Log the incoming exhibition object (for debugging)
			System.out.println("Received Exhibition: " + exhibition);

			site.getExhibitionRepo().add(exhibition);

			// Return success response
			return ResponseEntity.ok(exhibition); // Return the saved exhibition
		} catch (Exception e) {
			// Handle errors (e.g., log them and send an error response)
			System.err.println("Error saving exhibition: " + e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Error saving exhibition: " + e.getMessage());
		}
	}
}
