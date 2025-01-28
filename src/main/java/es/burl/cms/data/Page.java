package es.burl.cms.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Data
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE)
@Builder(builderClassName = "Builder", toBuilder = true)
@JsonDeserialize(builder = Page.Builder.class)
public class Page {

//	private final String title;
//	private final String url;
	private final MenuItem menuItem;
	private final String content;
	private final Painting insetImage; //TODO: allow modification / upload of inset image
	private final boolean showInMenu;
	private Gallery gallery;

	@JsonPOJOBuilder(withPrefix = "") // Removes "set" prefix from builder methods
	public static class Builder {
//		private String title = "New Page";
//		private String url = "url";
//		private int order = Integer.MAX_VALUE;
		private MenuItem menuItem = MenuItem.builder().build();
		private String content = null;
		private Painting insetImage = null;
		private boolean showInMenu = false;
		private Gallery gallery = new Gallery.Builder().build();

		public Builder fromPage(Page page){
//			this.title = page.title;
//			this.url = page.url;
//			this.order = page.order;
			this.menuItem = page.menuItem;
			this.content = page.content;
			this.insetImage = page.insetImage;
			this.showInMenu = page.showInMenu;
			this.gallery = page.gallery;
			return this;
		}
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
