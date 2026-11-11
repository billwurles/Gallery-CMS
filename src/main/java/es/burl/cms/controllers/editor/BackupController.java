package es.burl.cms.controllers.editor;

import es.burl.cms.backup.JsonBackup;
import es.burl.cms.backup.VersionDiff;
import es.burl.cms.builder.ThymeleafSiteBuilder;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaTypeFactory;

import java.io.IOException;
import java.nio.file.Path;
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
	private final ThymeleafSiteBuilder builder;

	@Autowired
	public BackupController(@Qualifier("getSite") Site site, @Qualifier("getPublishService") JsonBackup backups,
							ThymeleafSiteBuilder builder) {
		this.site = site;
		this.backups = backups;
		this.builder = builder;
	}

	/**
	 * What restoring a version would change: from the site now (or another version, ?from=<id>) to this one.
	 */
	@GetMapping("/{id}/compare")
	public String compare(@PathVariable String id, @RequestParam(name = "from", defaultValue = "current") String from, Model model) {
		List<JsonBackup.Snapshot> versions = backups.snapshots();
		JsonBackup.Snapshot version = versions.stream().filter(v -> v.id().equals(id)).findFirst()
				.orElseThrow(() -> new NotFoundException("The version " + id));
		Site fromSite = from.equals("current") ? site : versions.stream().anyMatch(v -> v.id().equals(from))
				? backups.loadVersion(from) : null;
		if (fromSite == null) throw new NotFoundException("The version " + from);
		model.addAttribute("menuItems", site.getMenuItems());
		model.addAttribute("version", version);
		model.addAttribute("from", from);
		model.addAttribute("fromVersion", versions.stream().filter(v -> v.id().equals(from)).findFirst().orElse(null));
		model.addAttribute("versions", versions);
		model.addAttribute("diff", VersionDiff.between(fromSite, backups.loadVersion(id)));
		return "editor/Compare";
	}

	/** A small copy of an image as a version had it (from the archive, so deleted paintings still show). */
	@GetMapping("/{id}/image")
	public ResponseEntity<Resource> versionImage(@PathVariable String id, @RequestParam String path) throws IOException {
		if (backups.snapshots().stream().noneMatch(v -> v.id().equals(id))) return ResponseEntity.notFound().build();
		Path image = backups.versionImage(id, path);
		if (image == null) return ResponseEntity.notFound().build();
		Path thumb = builder.thumbnail(image);
		return ResponseEntity.ok()
				.contentType(MediaTypeFactory.getMediaType(thumb.getFileName().toString()).orElse(MediaType.IMAGE_JPEG))
				.body(new FileSystemResource(thumb));
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
