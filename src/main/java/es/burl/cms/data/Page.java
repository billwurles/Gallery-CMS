package es.burl.cms.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(builderClassName = "Builder", toBuilder = true)
@JsonDeserialize(builder = Page.Builder.class)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class Page {

	private final MenuItem menuItem;
	private final String content;
	private final Painting insetImage; //TODO: allow modification / upload of inset image
	private final boolean showInMenu;
	private Gallery gallery;

	@JsonPOJOBuilder(withPrefix = "")
	public static class Builder {
		private MenuItem menuItem = MenuItem.builder().build();
		private String content = null;
		private Painting insetImage = null;
		private boolean showInMenu = false;
		private Gallery gallery = new Gallery.Builder().build();
	}

	public MenuItem getMenuItem(){
		return menuItem;
	}

	public void addPaintingToGallery(Painting painting) {
		gallery.addPainting(painting);
	}

	public boolean hasContent() {
		if(this.content != null){
			return !this.content.replaceAll("\\s+", "").isEmpty();
		} else return false;
	}

	public boolean hasInsetImage() {
		if(this.insetImage != null){
			return !this.insetImage.getFilename().isEmpty();
		} else return false;
	}

	public boolean hasGallery() {
		if(this.gallery != null){
			return !this.gallery.isEmpty();
		} else return false;
	}
}
