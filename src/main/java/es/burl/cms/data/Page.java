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

	private final String title;
	private final String url;
	@Setter
	private int order;
	private final String content;
	private final Painting insetImage;
	private final boolean showInMenu; // = true;
	private Gallery gallery;

	@JsonPOJOBuilder(withPrefix = "") // Removes "set" prefix from builder methods
	public static class Builder {
		private String title = "New Page";
		private String url = "url";
		private int order = Integer.MAX_VALUE;
		private String content = null;
		private Painting insetImage = null;
		private boolean showInMenu = false;
		private Gallery gallery = new Gallery.Builder().build();

		public Builder fromPage(Page page){
			this.title = page.title;
			this.url = page.url;
			this.order = page.order;
			this.content = page.content;
			this.insetImage = page.insetImage;
			this.showInMenu = page.showInMenu;
			this.gallery = page.gallery;
			return this;
		}
	}

	public MenuItem getMenuItem(){
		return new MenuItem(order, title, url);
	}

	public void addPaintingToGallery(Painting painting) {
		gallery.addPainting(painting);
	}

	public boolean hasContent() {
		return this.content == null || this.content.replaceAll("\\s+", "").isEmpty();
	}

	public boolean hasInsetImage() {
		return this.insetImage == null || !this.insetImage.getFilename().isEmpty();
	}

	public boolean hasGallery() {
		return this.gallery == null || this.gallery.isEmpty();
	}
}
