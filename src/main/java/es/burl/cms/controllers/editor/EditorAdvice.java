package es.burl.cms.controllers.editor;

import es.burl.cms.data.Site;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.ModelAndView;

/** Shared by every editor page: the site name for the header, and the not-found page. */
@Slf4j
@ControllerAdvice
public class EditorAdvice {

	private final Site site;

	@Autowired
	public EditorAdvice(@Qualifier("getSite") Site site) {
		this.site = site;
	}

	@ModelAttribute("siteName")
	public String siteName() {
		return site.getName();
	}

	@ExceptionHandler(NotFoundException.class)
	public ModelAndView notFound(NotFoundException e) {
		log.debug("Not found: {}", e.getMessage());
		ModelAndView view = new ModelAndView("editor/NotFound", HttpStatus.NOT_FOUND);
		view.addObject("message", e.getMessage());
		view.addObject("menuItems", site.getMenuItems());
		view.addObject("siteName", site.getName());
		return view;
	}
}
