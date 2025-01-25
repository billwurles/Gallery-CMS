package es.burl.cms.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Data
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE)
public class Page {

	private String title;
	private String url;
	@Setter
	private int order;
	private String content;
	private boolean showInMenu; // = true;
	private Gallery gallery;

	public Page(@JsonProperty("title") String title,
				@JsonProperty("url") String url,
				@JsonProperty("order") int order,
				@JsonProperty("content") String content,
				@JsonProperty("showInMenu") boolean showInMenu,
				@JsonProperty("gallery") Gallery gallery) {
		this.title = title;
		this.url = url;
		this.order = order;
		this.content = content;
		this.showInMenu = showInMenu;
		this.gallery = gallery;
	}

	public void addPaintingToGallery(Painting painting){
		gallery.addPainting(painting);
	}

	public boolean hasGallery() {
		return this.gallery.isEmpty();
	}
}
