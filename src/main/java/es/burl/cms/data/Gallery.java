package es.burl.cms.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public class Gallery {

	@JsonProperty("gallery")
	private Map<String, Painting> gallery;

	@JsonCreator
	public Gallery(@JsonProperty("gallery") Map<String, Painting> gallery){
		this.gallery = gallery;
	}

	public Gallery(List<Painting> paintings){
		this.gallery = paintings.stream()
				.collect(Collectors.toMap(Painting::getFilename, painting -> painting));
	}

	protected void addPainting(Painting painting) {
		gallery.put(String.valueOf(painting.getOrder()), painting);
	}

	public List<Painting> getGalleryInOrder(){
		return gallery.values().stream()
				.sorted(Comparator.comparingInt(Painting::getOrder))
				.collect(Collectors.toUnmodifiableList());
	}
}
