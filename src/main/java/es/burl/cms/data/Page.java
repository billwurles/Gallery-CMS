package es.burl.cms.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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
	private final boolean showInMenu; // = true;
	private Gallery gallery;

	@JsonPOJOBuilder(withPrefix = "") // Removes "set" prefix from builder methods
	public static class Builder {
		private String title = "New Page";
		private String url = "url";
		private int order = 100;
		private String content = "";
		private boolean showInMenu = false;
		private Gallery gallery = new Gallery.Builder().build();
		private List<Exhibition> exhibitions = new ArrayList<>();

		public Builder fromPage(Page page){
			this.title = page.title;
			this.url = page.url;
			this.order = page.order;
			this.content = page.content;
			this.showInMenu = page.showInMenu;
			this.gallery = page.gallery;
			return this;
		}
	}

//	public Page(@JsonProperty("title") String title, @JsonProperty("url") String url, @JsonProperty("order") int order, @JsonProperty("content") String content, @JsonProperty("showInMenu") boolean showInMenu, @JsonProperty("gallery") Gallery gallery, @JsonProperty("exhibitions") List<Exhibition> exhibitions) {
//		this.title = title;
//		this.url = url;
//		this.order = order;
//		this.content = content;
//		this.showInMenu = showInMenu;
//		this.gallery = gallery;
//		exhibitions.sort(Comparator.comparing(Exhibition::getDate));
//		this.exhibitions = exhibitions;
//	}

	public void addPaintingToGallery(Painting painting) {
		gallery.addPainting(painting);
	}

	public boolean hasGallery() {
		return this.gallery.isEmpty();
	}
}
