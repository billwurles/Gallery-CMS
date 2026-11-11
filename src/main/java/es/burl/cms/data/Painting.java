package es.burl.cms.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Data;

import java.util.UUID;

@Data
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE)
@lombok.Builder(builderClassName = "Builder", toBuilder = true)
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
		private String title = "";
		private String filename = "";
		private String dimensions = "";
		private String medium = "";
		private boolean sold = false;
		private int order = 100;
	}
}
