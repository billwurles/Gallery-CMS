package es.burl.cms.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Data;

@Data
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE)
@Builder(builderClassName = "Builder", toBuilder = true)
@JsonDeserialize(builder = Painting.Builder.class)
public class Painting {
	private final String title;
	private final String filename;
	private final String dimensions;
	private final String medium;
	private final boolean sold;// = false;
	private final int order;

	@JsonPOJOBuilder(withPrefix = "") // Removes "set" prefix from builder methods
	public static class Builder {
		private String title = "title";
		private String filename = "file.no";
		private String dimensions = "";
		private String medium = "";
		private boolean sold = false;
		private int order = 100;
	}

//	private Painting(Builder builder) {
//		this.title = builder.title;
//		this.filename = builder.filename;
//		this.dimensions = builder.dimensions;
//		this.medium = builder.medium;
//		this.sold = builder.sold;
//		this.order = builder.order;
//	}

//	@JsonCreator
//	public Painting(@JsonProperty("title") String title, @JsonProperty("filename") String filename, @JsonProperty("dimensions") String dimensions, @JsonProperty("medium") String medium, @JsonProperty("sold") boolean sold, @JsonProperty("order") int order) {
//		this.title = title;
//		this.filename = filename;
//		this.dimensions = dimensions;
//		this.medium = medium;
//		this.sold = sold;
//		this.order = order;
//	}

	public String toString() {
		return title;
	}
}
