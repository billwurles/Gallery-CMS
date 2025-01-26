package es.burl.cms.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import java.util.*;

@Data
@Getter
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
@Builder(builderClassName = "Builder", toBuilder = true)
@JsonDeserialize(builder = Gallery.Builder.class)
public class Gallery {

	@JsonProperty("gallery")
	private final Map<String, Painting> gallery;

	//TODO: A boolean flag of whether or not to display titles / other metadata

	@JsonPOJOBuilder(withPrefix = "") // Removes "set" prefix from builder methods
	public static class Builder {
		private Map<String, Painting> gallery = new HashMap<>();
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
