package es.burl.cms.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Data
@Getter
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class Gallery {

	@JsonProperty("gallery")
	private final Map<String, Painting> gallery;

	//TODO: A boolean flag of whether or not to display titles / other metadata

	@JsonCreator
	public Gallery(@JsonProperty("gallery") Map<String, Painting> gallery) {
		this.gallery = gallery;
	}

	public Painting getPainting(String filename) {
		return gallery.get(filename);
	}

	protected void addPainting(Painting painting) {
		gallery.put(painting.getFilename(), painting);
	}

	public List<Painting> getGalleryInOrder() {
		return gallery.values().stream().sorted(Comparator.comparingInt(Painting::getOrder)).toList();
	}

	public boolean isEmpty() {
		return gallery.isEmpty();
	}
}
