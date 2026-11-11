package es.burl.cms.controllers.editor;

import es.burl.cms.backup.BackupSite;
import es.burl.cms.data.MenuItem;
import es.burl.cms.data.Page;
import es.burl.cms.data.Site;
import es.burl.cms.data.Slugs;
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
	private final BackupSite saveService;

	@Autowired
	public PageController(@Qualifier("getSite") Site site,
						  @Qualifier("getGalleryRoot") Path galleryRoot,
						  @Qualifier("getSaveService") BackupSite saveService) {
		this.site = site;
		this.galleryRoot = galleryRoot;
		this.saveService = saveService;
	}

	@GetMapping(value = {"", "/"})
	public String newPage(Model model) {
		model.addAttribute("menuItems", site.getMenuItems());
		model.addAttribute("message", "Creating new page - save first before adding pictures to the Gallery");
		model.addAttribute("newPage", true);
		model.addAttribute("page", Page.builder().build());
		return "editor/EditPage";
	}

	//TODO: Have a live flag so that DIY pages aren't live
	//TODO: ensure that home / exhibitions cannot be overwritten (should be ok?)

	@PostMapping(value = {"", "/"}) //TODO: How to handle gallery before page is saved
	public String saveNewPage(@RequestParam String title, @RequestParam String url, @RequestParam String content,
							  @RequestParam(required = false) Boolean showContent,
							  @RequestParam(required = false) Boolean showGallery, Model model) {
		content = cleanContent(content);
		String urlProblem = Slugs.pageProblem(url);
		if (urlProblem != null) {
			model.addAttribute("menuItems", site.getMenuItems());
			model.addAttribute("error", urlProblem);
			model.addAttribute("newPage", true);
			model.addAttribute("page", Page.builder()
					.menuItem(MenuItem.builder().url(url).title(title).build())
					.content(content)
					.showContent(showContent == null || showContent)
					.showGallery(showGallery == null || showGallery)
					.build());
			return "editor/EditPage";
		}
		// Save the page content (title, url, content)

		log.debug("Saving new page: {}"+title);
		model.addAttribute("menuItems", site.getMenuItems());

		for(MenuItem menuItem : site.getMenuItems()){
			if(menuItem.getUrl().equals(url)){
				model.addAttribute("error", "The address \"" + url + "\" is already used by another page");
				model.addAttribute("newPage", true);
				model.addAttribute("page", Page.builder()
						.menuItem(MenuItem.builder()
								.url(url)
								.title(title).build())
						.content(content)
						.showContent(showContent == null || showContent)
						.showGallery(showGallery == null || showGallery)
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
				.showContent(showContent == null || showContent)
				.showGallery(showGallery == null || showGallery)
				.build()
		);

		// Pass the saved content back to the view
		Page page = site.getPage(url);
		model.addAttribute("message", "Content saved successfully!");
		model.addAttribute("page", page); // Pass the saved content back to the view
		model.addAttribute("hasGallery", page.hasGallery());
		saveService.backup(site);
		return "redirect:/page/" + url;
	}

	@GetMapping(value = {"/{pageUrl}/", "/{pageUrl}"}) // TODO: make it work with trailing slash
	public String editPage(@PathVariable("pageUrl") String pageUrl,
						   @RequestParam(name = "edit", required = false) String edit, Model model) {
		log.debug("Editing page: {}", pageUrl);
		model.addAttribute("menuItems", site.getMenuItems());
		Page page = site.getPage(pageUrl);

		if (page == null) throw new NotFoundException("The page " + pageUrl);

		// A gallery page with no text opens on its gallery, unless the text editor was asked for (?edit=text)
		if (!"text".equals(edit) && !page.hasContent() && page.hasGallery()) {
			return "redirect:/page/" + pageUrl + "/gallery";
		}
		model.addAttribute("message", "Editing page " + page.getMenuItem().getTitle());
		model.addAttribute("page", page);
		model.addAttribute("hasGallery", page.hasGallery());
		model.addAttribute("newPage", false);
		return "editor/EditPage";
	}

	@PostMapping(value = {"/{pageUrl}/", "/{pageUrl}"})
	public String saveContent(@PathVariable("pageUrl") String originalPageUrl, @RequestParam String title, @RequestParam String url,
							  @RequestParam String content,
							  @RequestParam(required = false) Boolean showContent,
							  @RequestParam(required = false) Boolean showGallery, Model model) {
		content = cleanContent(content);
		// Save the page content (title, url, content)
		log.debug("Saving edited page: "+originalPageUrl);
		model.addAttribute("menuItems", site.getMenuItems());

		Page originalPage = site.getPage(originalPageUrl);
		if (originalPage == null) throw new NotFoundException("The page " + originalPageUrl);
		Page newPage = originalPage.toBuilder()
				.menuItem(originalPage.getMenuItem().toBuilder()
						.title(title)
						.url(url)
						.build())
				.content(content)
				// Left out of the request: keep the page\'s current setting
				.showContent(showContent == null ? originalPage.isShowContent() : showContent)
				.showGallery(showGallery == null ? originalPage.isShowGallery() : showGallery)
				.build();

		String urlProblem = originalPageUrl.equals(url) ? null : Slugs.pageProblem(url);
		if (urlProblem != null) {
			model.addAttribute("error", urlProblem);
			model.addAttribute("page", newPage);
			model.addAttribute("hasGallery", newPage.hasGallery());
			model.addAttribute("newPage", false);
			return "editor/EditPage";
		}

		if (!originalPageUrl.equals(url)) {
			for(MenuItem menuItem : site.getMenuItems()){
				if(menuItem != originalPage.getMenuItem() && menuItem.getUrl().equals(newPage.getUrl())){
					model.addAttribute("error", "The address \"" + newPage.getMenuItem().getUrl() + "\" is already used by another page");
					model.addAttribute("page", newPage);
					model.addAttribute("hasGallery", newPage.hasGallery());
					return "editor/EditPage";
				}
			}
			Filesystem.moveGalleryToNewUrl(originalPageUrl, newPage.getUrl(), galleryRoot);
			site.removePage(originalPageUrl);
		}
		site.addNewPage(newPage);
		saveService.backup(site);
		return "redirect:/page/" + url;
	}

	@DeleteMapping(value = {"/{pageUrl}/", "/{pageUrl}"})
	public ResponseEntity<?> deletePage(@PathVariable("pageUrl") String pageUrl) {
		if (site.removePage(pageUrl)) {
			site.reorderPageOrder();
			saveService.backup(site);
			// The gallery folder stays: its images are archived with the backups, and the backup cleanup
			// removes them from the gallery once nothing live uses them
			return ResponseEntity.ok("Page " + pageUrl + " was deleted");
		}
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body("The page " + pageUrl + " was not found");
	}

	/** Content with nothing to see (what the editor leaves when its text is deleted) is stored as empty. */
	private static String cleanContent(String content) {
		return Page.isBlankHtml(content) ? "" : content;
	}
}
