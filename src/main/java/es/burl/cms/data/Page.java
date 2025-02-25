package es.burl.cms.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import es.burl.cms.helper.Filesystem;
import lombok.Builder;
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
@Builder(builderClassName = "Builder", toBuilder = true)
@JsonDeserialize(builder = Page.Builder.class)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class Page {

	private final MenuItem menuItem;
	private final String content;
	private Gallery gallery;

	@JsonPOJOBuilder(withPrefix = "")
	public static class Builder {
		private MenuItem menuItem = MenuItem.builder().build();
		private String content = null;
		private Gallery gallery = new Gallery.Builder().build();
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

	public boolean hasContent() {
		if(this.content != null){
			return !this.content.replaceAll("\\s+", "").isEmpty();
		} else return false;
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
