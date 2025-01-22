package es.burl.cms.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.*;
import java.util.stream.Collectors;

@Getter
public class Gallery {

	@JsonProperty("gallery")
	private final List<Painting> gallery;

	@JsonCreator
	public Gallery(@JsonProperty("gallery") Map<String, Painting> gallery){
		this.gallery = new ArrayList<>(gallery.values());
	}

	public Gallery(List<Painting> paintings){
		this.gallery = paintings;
	}

	protected void addPainting(Painting painting) {
		gallery.add(painting);
	}

	public List<Painting> getGalleryInOrder(){
		return gallery.stream()
				.sorted(Comparator.comparingInt(Painting::getOrder))
				.toList();
	}

	public boolean isEmpty(){
		return gallery.isEmpty();
	}

	public static Gallery Empty(){
		return new Gallery(Collections.EMPTY_LIST);
	}
}
