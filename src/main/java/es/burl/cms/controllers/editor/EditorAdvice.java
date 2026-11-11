package es.burl.cms.controllers.editor;

import es.burl.cms.backup.JsonBackup;
import es.burl.cms.data.Site;
import es.burl.cms.helper.UploadedImages;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
	private final JsonBackup backups;

	/** For tests that don't need the publish status. */
	public EditorAdvice(Site site) {
		this(site, null);
	}

	@Autowired
	public EditorAdvice(@Qualifier("getSite") Site site, @Qualifier("getPublishService") JsonBackup backups) {
		this.site = site;
		this.backups = backups;
	}

	/** Whether there's anything to publish, for the Publish button and the status line on every page. */
	@ModelAttribute("publishStatus")
	public JsonBackup.PublishStatus publishStatus() {
		return backups == null ? null : backups.status(site);
	}

	@ModelAttribute("siteName")
	public String siteName() {
		return site.getName();
	}

	/** An upload that isn't a usable image: 400 with a message the editor shows as it is. */
	@ExceptionHandler(UploadedImages.InvalidUploadException.class)
	public ResponseEntity<String> badUpload(UploadedImages.InvalidUploadException e) {
		log.info("Rejected upload: {}", e.getMessage());
		return ResponseEntity.badRequest().contentType(MediaType.TEXT_PLAIN).body(e.getMessage());
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
