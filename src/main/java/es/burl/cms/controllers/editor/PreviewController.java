package es.burl.cms.controllers.editor;

import es.burl.cms.builder.ThymeleafSiteBuilder;
import es.burl.cms.data.Site;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The draft site, as it would be published, at /preview/ in the editor (so behind the same login).
 * Opening /preview rebuilds it from the current draft; its pages, images and styles are then served from
 * a folder next to html_root that nginx never sees.
 */
@Slf4j
@Controller
public class PreviewController {

	static final String PREVIEW_PATH = "/preview";

	private final Site site;
	private final ThymeleafSiteBuilder builder;
	private final Path previewRoot;

	@Autowired
	public PreviewController(@Qualifier("getSite") Site site, ThymeleafSiteBuilder builder,
							 @Qualifier("getHtmlRoot") Path htmlRoot) {
		this.site = site;
		this.builder = builder;
		this.previewRoot = htmlRoot.toAbsolutePath().normalize().resolveSibling("preview_root");
	}

	/** Builds the preview from the current draft, then shows its home page. */
	@GetMapping(PREVIEW_PATH)
	public String preview() throws IOException {
		// The editor's own address, as the browser sees it (it includes the proxy's prefix)
		String editorBase = ServletUriComponentsBuilder.fromCurrentContextPath().build().getPath();
		editorBase = editorBase == null ? "" : editorBase.replaceAll("/+$", "");
		builder.buildPreview(site, previewRoot, editorBase + PREVIEW_PATH, editorBase + "/");
		return "redirect:" + PREVIEW_PATH + "/";
	}

	/** One file of the last preview build: a folder serves its index.html, and anything missing the 404 page. */
	@GetMapping(PREVIEW_PATH + "/**")
	public ResponseEntity<Resource> file(HttpServletRequest request) {
		String prefix = request.getContextPath() + PREVIEW_PATH + "/";
		String uri = request.getRequestURI();
		String relative = uri.startsWith(prefix) ? URLDecoder.decode(uri.substring(prefix.length()), StandardCharsets.UTF_8) : "";
		if (!Files.isDirectory(previewRoot)) {
			return ResponseEntity.status(HttpStatus.FOUND).header("Location", request.getContextPath() + PREVIEW_PATH).build();
		}
		Path file = previewRoot.resolve(relative).normalize();
		if (!file.startsWith(previewRoot)) return notFound();
		if (Files.isDirectory(file)) file = file.resolve("index.html");
		if (!Files.isRegularFile(file)) return notFound();
		return ResponseEntity.ok()
				.cacheControl(CacheControl.noStore())
				.contentType(MediaTypeFactory.getMediaType(file.getFileName().toString()).orElse(MediaType.APPLICATION_OCTET_STREAM))
				.body(new FileSystemResource(file));
	}

	private ResponseEntity<Resource> notFound() {
		Path page = previewRoot.resolve("404.html");
		if (!Files.isRegularFile(page)) return ResponseEntity.notFound().build();
		return ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(MediaType.TEXT_HTML).body(new FileSystemResource(page));
	}
}
