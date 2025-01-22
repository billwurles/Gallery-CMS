package es.burl.cms.editor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.burl.cms.data.MenuItem;
import es.burl.cms.data.Page;
import es.burl.cms.data.Painting;
import es.burl.cms.data.Site;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Slf4j
@Controller
public class EditorController {

	private final Site site;

	@Autowired
	public EditorController(Site site) {
		this.site = site;
	}


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

}
