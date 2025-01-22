package es.burl.cms.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Painting {
	private final String title;
	private final String filename;
	private final String dimensions;
	private final boolean sold;// = false;
	private final int order;

	@JsonCreator
	public Painting(
			@JsonProperty("title") String title,
			@JsonProperty("filename") String filename,
			@JsonProperty("dimensions") String dimensions,
			@JsonProperty("sold") boolean sold,
			@JsonProperty("order") int order) {
		this.title = title;
		this.filename = filename;
		this.dimensions = dimensions;
		this.sold = sold;
		this.order = order;
	}

	public String toString(){
		return title;
	}
}
