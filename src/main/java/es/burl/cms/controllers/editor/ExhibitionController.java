package es.burl.cms.controllers.editor;

import es.burl.cms.data.Exhibition;
import es.burl.cms.data.Site;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/exhibitions")
@Controller
public class ExhibitionController {

	private final Site site;

	@Autowired
	public ExhibitionController(Site site) {
			this.site = site;
		}

	@GetMapping(value = {"", "/"})
	public String getExhibitions(
			@RequestParam(name = "page", defaultValue = "1") int page,
			Model model) {
		int postsPerPage = 7;
		int totalPages = site.getExhibitionRepo().getTotalPages(postsPerPage);

		model.addAttribute("exhibitions", site.getExhibitionRepo());
		model.addAttribute("exhibitionsPage", site.getExhibitionRepo().getExhibitionsPage(page, postsPerPage));
		model.addAttribute("page", page);
		model.addAttribute("totalPages", totalPages);
		model.addAttribute("menuItems", site.getMenuItems());

		return "editor/ViewExhibitions";
	}

	@GetMapping("/{id}")
	public String editExhibition(@PathVariable("id") String id, Model model){
		Exhibition exhibition;
		if(id.equals("new")){
			exhibition = Exhibition.builder().build();
		} else {
			exhibition = site.getExhibitionRepo().get(id);
		}
		model.addAttribute("name", site.getName());
		model.addAttribute("menuItems", site.getMenuItems());
		model.addAttribute("exhibition", exhibition);
		return "editor/EditExhibition";
	}

	// Save method to handle POST requests
	@PostMapping("/{id}/save")
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
