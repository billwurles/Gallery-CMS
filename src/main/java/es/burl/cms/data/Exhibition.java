package es.burl.cms.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Data
@Getter
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
@Builder(builderClassName = "Builder", toBuilder = true)
@JsonDeserialize(builder = Exhibition.Builder.class)
public class Exhibition {

	private final String title;
	private final String id;
	private final Date date;
	private final String content;

	@JsonPOJOBuilder(withPrefix = "") // Removes "set" prefix from builder methods
	public static class Builder {
		private String title = "New Exhibition";
		private String id = UUID.randomUUID().toString();
		private Date date = Date.from(Instant.now());
		private String content = "";
	}
}
