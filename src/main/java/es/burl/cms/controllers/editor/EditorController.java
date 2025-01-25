package es.burl.cms.controllers.editor;

import es.burl.cms.backup.JsonBackup;
import es.burl.cms.data.Site;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.nio.file.Path;
import java.util.Map;

@Slf4j
@Controller
public class EditorController {

	private final Site site;
	private final Path backupPath;

	@Autowired
	public EditorController(Site site, Path backupPath) {
		this.site = site;
		this.backupPath = backupPath;
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
			model.addAttribute("message", "Site saved successfully");
		} catch (RuntimeException e) {
			model.addAttribute("message", "Error saving site");
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

	//TODO: Exhibition / Blog page

}
