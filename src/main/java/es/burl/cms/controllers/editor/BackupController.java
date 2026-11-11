package es.burl.cms.controllers.editor;

import es.burl.cms.backup.JsonBackup;
import es.burl.cms.data.Site;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;
import java.util.Map;

/** The Backups page: every kept version, to restore or download, and a check of the image archive. */
@Slf4j
@Controller
@RequestMapping("/backups")
public class BackupController {

	private final Site site;
	private final JsonBackup backups;

	@Autowired
	public BackupController(@Qualifier("getSite") Site site, @Qualifier("getPublishService") JsonBackup backups) {
		this.site = site;
		this.backups = backups;
	}

	@GetMapping(value = {"", "/"})
	public String list(Model model) {
		model.addAttribute("menuItems", site.getMenuItems());
		model.addAttribute("snapshots", backups.snapshots());
		return "editor/Backups";
	}

	/** Restores one kept version, saving the current site first so this can be undone. */
	@PostMapping("/{file}/restore")
	@ResponseBody
	public ResponseEntity<Map<String, Object>> restore(@PathVariable String file) {
		JsonBackup.Restored restored;
		synchronized (site) {
			try {
				backups.backup(site); // the undo point: the newest draft is what was there before
				restored = backups.restoreSnapshot(file);
			} catch (IllegalArgumentException e) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
			}
			site.replaceWith(restored.site());
			backups.backup(site);
		}
		return ResponseEntity.ok(Map.of("missing", restored.missing()));
	}

	@GetMapping("/{file}/download")
	public ResponseEntity<StreamingResponseBody> download(@PathVariable String file) {
		boolean known = backups.snapshots().stream().anyMatch(s -> s.id().equals(file));
		if (!known) return ResponseEntity.notFound().build();
		String name = file.replaceAll("\\.json$", "") + ".zip";
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType("application/zip"))
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + name + "\"")
				.body(out -> backups.writeZip(file, out));
	}

	@GetMapping("/check")
	@ResponseBody
	public JsonBackup.IntegrityReport check() {
		return backups.checkIntegrity(site);
	}

	/** Once at startup: a line in the log saying whether every image is where it should be. */
	@EventListener(ApplicationReadyEvent.class)
	public void checkAtStartup() {
		JsonBackup.IntegrityReport report = backups.checkIntegrity(site);
		if (report.ok()) {
			log.info("Image check: {} archived image(s), all intact; every live painting has its file", report.archived());
		} else {
			log.warn("Image check: {} corrupt in the archive {}, {} live painting(s) missing a file {}, {} image(s) kept backups need but the archive lacks {}",
					report.corrupt().size(), first(report.corrupt()), report.missingFromGallery().size(), first(report.missingFromGallery()),
					report.missingFromArchive().size(), first(report.missingFromArchive()));
		}
	}

	private static List<String> first(List<String> list) {
		return list.subList(0, Math.min(5, list.size()));
	}
}
