package es.burl.cms.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonPOJOBuilder;
import es.burl.cms.helper.Filesystem;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Slf4j
@Data
@lombok.Builder(builderClassName = "Builder", toBuilder = true)
@JsonDeserialize(builder = Page.Builder.class)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class Page {

	private final MenuItem menuItem;
	private final String content;
	private Gallery gallery;
	/** Whether the published page shows its text / its gallery. Hiding keeps them, so they can come back. */
	private final boolean showContent;
	private final boolean showGallery;

	@JsonPOJOBuilder(withPrefix = "")
	public static class Builder {
		private MenuItem menuItem = MenuItem.builder().build();
		private String content = null;
		private Gallery gallery = new Gallery.Builder().build();
		// Backups from before these settings existed show both
		private boolean showContent = true;
		private boolean showGallery = true;

		/** Every way a page is made (editor, backup restore, toBuilder) drops a stray editor field. */
		public Builder content(String content) {
			this.content = Html.clean(content);
			return this;
		}
	}

	public MenuItem getMenuItem() {
		return menuItem;
	}

	public String getUrl() {
		return this.menuItem.getUrl();
	}

	public String getTitle() {
		return this.menuItem.getTitle();
	}

	public void addPaintingToGallery(Painting painting) {
		gallery.addPainting(painting);
	}

	/** True when the content has something to see: visible text, or an image, video or table. */
	public boolean hasContent() {
		return !isBlankHtml(content);
	}

	/** HTML with nothing to see; see {@link Html#isBlank}. */
	public static boolean isBlankHtml(String html) {
		return Html.isBlank(html);
	}

	/** The published page shows its text: it has some, and it isn't hidden. */
	public boolean showsContent() {
		return showContent && hasContent();
	}

	/** The published page shows its gallery: it has paintings, and it isn't hidden. */
	public boolean showsGallery() {
		return showGallery && hasGallery();
	}

	public boolean hasGallery() {
		if(this.gallery != null){
			return !this.gallery.isEmpty();
		} else return false;
	}

	public boolean saveGallery(Gallery galleryRequest, Path galleryRoot) {
//		Gallery newGallery = Gallery.builder().build(); //TODO: stop it being ajax so that the data is updated in the view
		if (galleryRequest != null) {
//			for (Painting painting : galleryRequest.getGalleryInOrder()) {
//
//				Painting oldPainting = getGallery().getPainting(painting.getFilename());
//
//				String filename = painting.getFilename();
//				painting = painting.toBuilder().filename(filename).build();
//
//				// Construct the file path and save the image
//				Path uploadPath = galleryRoot.resolve(getUrl());
//				File image = uploadPath.resolve(oldPainting.getFilename()).toFile();
//				File newName = new File(image.getParent(), filename);
//				image.renameTo(newName);
//
//				newGallery.addPainting(painting);
//			}

			// Save the updated page with the new gallery
			setGallery(galleryRequest);

			return true;
		}
		return false;
	}

	public void removePainting(String filename){
		getGallery().getGallery().remove(filename);
	}

	public void sortPaintingsByAspectRatio(Path galleryRoot, boolean reversed) throws IOException {
		log.debug("Sorting images for {} by aspect ratio", getUrl());
		Filesystem.sortPaintingsByAspectRatio(this, galleryRoot, reversed);
	}
}
