package es.burl.cms.data;

import lombok.*;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

@AllArgsConstructor
@Getter
@EqualsAndHashCode
@ToString
public class Page {

	private String title;
	private String url;
	@Setter
	private int menuOrder;
	private String content;
	private boolean showInMenu; // = true;
	private Gallery gallery;

	public void addPaintingToGallery(Painting painting){
		gallery.addPainting(painting);
	}

	public void replaceGallery(Gallery gallery){
		this.gallery = gallery;
	}
}
