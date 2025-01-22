package es.burl.cms.editor;

import com.fasterxml.jackson.core.JsonProcessingException;
import es.burl.cms.data.MenuItem;
import es.burl.cms.data.Page;
import es.burl.cms.data.Site;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RequestMapping("/page")
@Controller
public class PageController {

	private final Site site;

	@Autowired
	public PageController(Site site) {
		this.site = site;
	}

	@GetMapping(value = { "", "/" })
	public String newPage(Model model){
		List<MenuItem> menu = site.getMenuItems();
		model.addAttribute("menuItems", site.getMenuItems());
		model.addAttribute("message", "Creating new page");
		model.addAttribute("page", new Page("","",-10,"",true, null));
		return "editor/EditPage";
	}

	@GetMapping("/{pageUrl}") // TODO: make it work with trailing slash
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
	@PostMapping("/{pageUrl}/save")
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
}
