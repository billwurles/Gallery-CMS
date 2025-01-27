package es.burl.cms.controllers.editor;

import es.burl.cms.data.Page;
import es.burl.cms.data.Site;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequestMapping("/page")
@Controller
public class PageController {

	private final Site site;

	@Autowired
	public PageController(Site site) {
		this.site = site;
	}

	@GetMapping(value = {"", "/"})
	public String newPage(Model model) {
		model.addAttribute("menuItems", site.getMenuItems());
		model.addAttribute("message", "Creating new page");
		model.addAttribute("newPage", true);
		model.addAttribute("page", Page.builder().build());
		return "editor/EditPage";
	}

	//TODO: Have a live flag (maybe instead of showInMenu) so that DIY pages aren't live

	@PostMapping("/save") //TODO: How to handle gallery before page is saved
	public String saveNewPage(@RequestParam String title, @RequestParam String url, @RequestParam String content, @RequestParam boolean showInMenu, Model model) {
		// Save the page content (title, url, content, showInMenu)

		site.addNewPage(Page.builder()
				.title(title)
				.url(url)
				.content(content)
				.showInMenu(showInMenu)
				.build()
		);

		// Pass the saved content back to the view
		Page page = site.getPage(url);
		model.addAttribute("message", "Content saved successfully!");
		model.addAttribute("page", page); // Pass the saved content back to the view
		model.addAttribute("hasGallery", page.hasGallery());
		model.addAttribute("menuItems", site.getMenuItems());
		return "redirect:/page/" + url;
	}

	//TODO: When you change page URL, you will need to update the folder structure of gallery
	@GetMapping("/{pageUrl}") // TODO: make it work with trailing slash
	public String editPage(@PathVariable("pageUrl") String pageUrl, Model model) {
		model.addAttribute("menuItems", site.getMenuItems());
		Page page = site.getPage(pageUrl);
		log.debug("Editing page: {}", page.toString());

		if (page != null) {
			model.addAttribute("message", "Editing page " + page.getTitle());
			model.addAttribute("page", page);  // Pass the page to the model
			model.addAttribute("hasGallery", page.hasGallery());
			return "editor/EditPage";
		} else {
			return "404";  // Return a "404.html" page if page not found
		}
	}

	//TODO: maybe spin off rich content editing to it's own page
	@PostMapping("/{pageUrl}/save")
	public String saveContent(@PathVariable("pageUrl") String originalPageUrl, @RequestParam String title, @RequestParam String url, @RequestParam String content, @RequestParam boolean showInMenu, Model model) {
		// Save the page content (title, url, content, showInMenu)

		site.addNewPage(Page.builder()
				.title(title)
				.url(url)
				.content(content)
				.showInMenu(showInMenu)
				.gallery(site.getPageGallery(originalPageUrl))
				.build()
		);

		if (!originalPageUrl.equals(url)) {
			site.removePage(originalPageUrl);
		}

		// Pass the saved content back to the view
		Page page = site.getPage(url);
		model.addAttribute("message", "Content saved successfully!");
		model.addAttribute("page", page); // Pass the saved content back to the view
		model.addAttribute("hasGallery", page.hasGallery());
		model.addAttribute("menuItems", site.getMenuItems());
		return "editor/EditPage";
	}
}
