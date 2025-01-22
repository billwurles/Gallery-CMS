package es.burl.cms.data;

import lombok.*;

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
	@NonNull
	@Setter
	private Gallery gallery;

	public void addPaintingToGallery(Painting painting){
		gallery.addPainting(painting);
	}

	public boolean hasGallery() {
		return this.gallery.isEmpty();
	}
}
