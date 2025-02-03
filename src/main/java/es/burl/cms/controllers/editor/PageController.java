package es.burl.cms.controllers.editor;

import es.burl.cms.data.MenuItem;
import es.burl.cms.data.Page;
import es.burl.cms.data.Site;
import es.burl.cms.helper.Filesystem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;

@Slf4j
@RequestMapping("/page")
@Controller
public class PageController {

	private final Site site;
	private final Path galleryRoot;

	@Autowired
	public PageController(Site site,
				@Qualifier("getGalleryRoot") Path galleryRoot) {
		this.site = site;
		this.galleryRoot = galleryRoot;
	}

	@GetMapping(value = {"", "/"})
	public String newPage(Model model) {
		model.addAttribute("menuItems", site.getMenuItems());
		model.addAttribute("message", "Creating new page");
		model.addAttribute("newPage", true);
		model.addAttribute("page", Page.builder().build());
		return "editor/EditPage";
	}

	//TODO: Have a live flag so that DIY pages aren't live
	//TODO: ensure that home / exhibitions cannot be overwritten (should be ok?)

	@PostMapping("/") //TODO: How to handle gallery before page is saved
	public String saveNewPage(@RequestParam String title, @RequestParam String url, @RequestParam String content, Model model) {
		// Save the page content (title, url, content)

		log.debug("Saving new page: {}"+title);
		model.addAttribute("menuItems", site.getMenuItems());

		for(MenuItem menuItem : site.getMenuItems()){
			if(menuItem.getUrl().equals(url)){
				model.addAttribute("message", "Page url ("+url+") is already in use");
				model.addAttribute("newPage", true);
				model.addAttribute("page", Page.builder()
						.menuItem(MenuItem.builder()
								.url(url)
								.title(title).build())
						.content(content)
						.build());
				return "editor/EditPage";
			}
		}
		site.addNewPage(Page.builder()
				.menuItem(MenuItem.builder()
						.title(title)
						.url(url)
						.order(site.getNextPageOrder())
						.build())
				.content(content)
				.build()
		);

		// Pass the saved content back to the view
		Page page = site.getPage(url);
		model.addAttribute("message", "Content saved successfully!");
		model.addAttribute("page", page); // Pass the saved content back to the view
		model.addAttribute("hasGallery", page.hasGallery());
		return "redirect:/page/" + url;
	}

	@GetMapping("/{pageUrl}") // TODO: make it work with trailing slash
	public String editPage(@PathVariable("pageUrl") String pageUrl, Model model) {
		log.debug("Editing page: {}", pageUrl);
		model.addAttribute("menuItems", site.getMenuItems());
		Page page = site.getPage(pageUrl);

		if (page != null) {
			model.addAttribute("message", "Editing page " + page.getMenuItem().getTitle());
			model.addAttribute("page", page);  // Pass the page to the model
			model.addAttribute("hasGallery", page.hasGallery());
			return "editor/EditPage";
		} else {
			return "404";  // Return a "error.html" page if page not found
		}
	}

	@PostMapping("/{pageUrl}")
	public String saveContent(@PathVariable("pageUrl") String originalPageUrl, @RequestParam String title, @RequestParam String url, @RequestParam String content, Model model) {
		// Save the page content (title, url, content)
		log.debug("Saving edited page: "+originalPageUrl);
		model.addAttribute("menuItems", site.getMenuItems());

		Page originalPage = site.getPage(originalPageUrl);
		Page newPage = originalPage.toBuilder()
				.menuItem(originalPage.getMenuItem().toBuilder()
						.title(title)
						.url(url)
						.build())
				.content(content)
				.build();

		if (!originalPageUrl.equals(url)) {
			for(MenuItem menuItem : site.getMenuItems()){
				if(menuItem != originalPage.getMenuItem() && menuItem.getUrl().equals(newPage.getUrl())){
					model.addAttribute("message", "Page URL " + newPage.getMenuItem().getUrl() +" is already in use");
					model.addAttribute("page", newPage);
					model.addAttribute("hasGallery", newPage.hasGallery());
					return "editor/EditPage";
				}
			}
			Filesystem.copyGalleryImagesToNewUrl(originalPageUrl, newPage.getUrl(), galleryRoot);
			site.removePage(originalPageUrl);
		}
		site.addNewPage(newPage);

		// Pass the saved content back to the view
		Page page = site.getPage(url);
		model.addAttribute("message", "Content saved successfully!");
		model.addAttribute("page", page); // Pass the saved content back to the view
		model.addAttribute("hasGallery", page.hasGallery());
		return "editor/EditPage";
	}

	@DeleteMapping("/{pageUrl}")
	public ResponseEntity<?> deletePage(@PathVariable("pageUrl") String pageUrl) {
		if(site.removePage(pageUrl)){
			site.reorderPageOrder();
			return ResponseEntity.ok("Page "+pageUrl+" was deleted");
		} else {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred during gallery upload");
		}
		//TODO: also delete gallery from filesystem (or temp backup?)
		//TODO: reset menuItems orders to remove blank space
	}
}
