package es.burl.cms.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.Comparator;
import java.util.List;

@Getter
@Data
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE)
public class Page {

	private final String title;
	private final String url;
	@Setter
	private int order;
	private final String content;
	private final boolean showInMenu; // = true;
	private Gallery gallery;
	private final List<Exhibition> exhibitions;

	public Page(@JsonProperty("title") String title, @JsonProperty("url") String url, @JsonProperty("order") int order, @JsonProperty("content") String content, @JsonProperty("showInMenu") boolean showInMenu, @JsonProperty("gallery") Gallery gallery, @JsonProperty("exhibitions") List<Exhibition> exhibitions) {
		this.title = title;
		this.url = url;
		this.order = order;
		this.content = content;
		this.showInMenu = showInMenu;
		this.gallery = gallery;
		exhibitions.sort(Comparator.comparing(Exhibition::getDate));
		this.exhibitions = exhibitions;
	}

	public void addPaintingToGallery(Painting painting) {
		gallery.addPainting(painting);
	}

	public void addExhibition(Exhibition exhibition) {
		this.exhibitions.add(exhibition);
	}

	public List<Exhibition> getExhibitions() {
		exhibitions.sort(Comparator.comparing(Exhibition::getDate));
		return exhibitions;
	}

	public List<Exhibition> getExhibitionsPage(int page, int postsPerPage) {
		exhibitions.sort(Comparator.comparing(Exhibition::getDate));
		int to = page * postsPerPage;
		int from = to - postsPerPage;
		if (to > exhibitions.size()) to = exhibitions.size();
		return exhibitions.subList(from, to);
	}

	public boolean hasGallery() {
		return this.gallery.isEmpty();
	}
}
