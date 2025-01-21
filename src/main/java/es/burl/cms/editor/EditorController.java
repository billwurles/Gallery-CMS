package es.burl.cms.editor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.burl.cms.data.MenuItem;
import es.burl.cms.data.Page;
import es.burl.cms.data.Painting;
import es.burl.cms.data.Site;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Controller
public class EditorController {

	Site site = Site.getFakeSite();

	@GetMapping("/")
	public String getHomePage(Model model){
		model.addAttribute("page", "home");
		model.addAttribute("name", site.getName());
		model.addAttribute("menuItems", site.getMenuItems());
		model.addAttribute("pages", site.getPagesInMenuOrder());
		return "editor/EditorHome";
	}

	@PostMapping("/rearrange")
	public String rearrangePages(@RequestParam List<Integer> order, Model model) {
		site.updatePageOrder(order);

		return "redirect:/"; // Redirect to the home after saving
	}

	@GetMapping("/newPage")
	public String newPage(Model model){
		List<MenuItem> menu = site.getMenuItems();
		model.addAttribute("menuItems", site.getMenuItems());
		model.addAttribute("message", "Creating new page");
		model.addAttribute("page", new Page("","",-10,"",true, null));
		return "editor/EditPage";
	}

	@GetMapping("/edit/{pageUrl}")
	public String editPage(@PathVariable("pageUrl") String pageUrl, Model model){
		model.addAttribute("menuItems", site.getMenuItems());
		Page page = site.getPage(pageUrl);
		log.debug("Editing page: {}", page.toString());

		if (page != null) {
			model.addAttribute("message", "Editing page "+page.getTitle());
			model.addAttribute("page", page);  // Pass the page to the model
			model.addAttribute("hasGallery", page.getGallery() != null);
			return "editor/EditPage";
		} else {
			return "404";  // Return a "404.html" page if page not found
		}
	}

	//TODO: maybe spin off rich content editing to it's own page
	@PostMapping("/edit/{pageUrl}/save")
	public String saveContent(@PathVariable("pageUrl") String originalPageUrl,
							  @RequestParam String title,
							  @RequestParam String url,
							  @RequestParam String content,
							  @RequestParam boolean showInMenu,
							  @RequestParam(required = false) String orderedPaintingTitles,
							  Model model) throws JsonProcessingException {
		// Save the page content (title, url, content, showInMenu)

		site.addNewPage(title, url, content, showInMenu, site.getPageGallery(originalPageUrl));

		if (!originalPageUrl.equals(url)) {
			site.removePage(originalPageUrl);
		}

		// Pass the saved content back to the view
		Page page = site.getPage(url);
		model.addAttribute("message", "Content saved successfully!");
		model.addAttribute("page", page); // Pass the saved content back to the view
		model.addAttribute("hasGallery", page.getGallery() != null);
		model.addAttribute("menuItems", site.getMenuItems());
		return "editor/EditPage";
	}

	//TODO: uploading of new images

	@GetMapping("/edit/{pageUrl}/gallery")
	public String editGallery(@PathVariable("pageUrl") String pageUrl, Model model){
		model.addAttribute("menuItems", site.getMenuItems());
		Page page = site.getPage(pageUrl);
		log.debug("Editing gallery for page: {}", page.toString());

		if (page != null) {
			model.addAttribute("message", "Editing gallery "+page.getTitle());
			model.addAttribute("page", page);  // Pass the page to the model
			model.addAttribute("hasGallery", page.getGallery() != null);
			return "editor/EditGallery";
		} else {
			return "404";  // Return a "404.html" page if page not found
		}
	}

	@PostMapping("/edit/{pageUrl}/gallery/save")
	public String saveGallery(@PathVariable("pageUrl") String pageUrl,
							  @RequestParam(required = false) String orderedPaintingTitles,
							  Model model) throws JsonProcessingException {
		// Save the page content (title, url, content, showInMenu)

		List<Painting> newGallery = null;
		if (orderedPaintingTitles != null) {

			// Deserialize the ordered painting titles from the JSON string
			List<String> orderedTitles = new ObjectMapper().readValue(orderedPaintingTitles, List.class);

			// Get the page object and update its gallery order
			Page page = site.getPage(pageUrl);
			if (page != null) {
				// Create a new list of paintings in the new order
				List<Painting> reorderedGallery = new ArrayList<>();

				// Map the ordered titles to the actual painting objects
				for (String titleInOrder : orderedTitles) {
					for (Painting painting : page.getGallery()) {
						if (painting.getTitle().equals(titleInOrder)) {
							reorderedGallery.add(painting);
							break;
						}
					}
				}

				// Update the gallery with the new order
				newGallery = reorderedGallery;
			}
		}
		Page page = site.getPage(pageUrl);
		site.addNewPage(page.getTitle(), pageUrl, page.getContent(), page.isShowInMenu(), newGallery);

		// Pass the saved content back to the view
		page = site.getPage(pageUrl);
		model.addAttribute("message", "Content saved successfully!");
		model.addAttribute("page", page); // Pass the saved content back to the view
		model.addAttribute("hasGallery", page.getGallery() != null);
		model.addAttribute("menuItems", site.getMenuItems());
		return "editor/EditGallery";
	}
}
