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

@Data
@Getter
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
@Builder(builderClassName = "Builder", toBuilder = true)
@JsonDeserialize(builder = Exhibition.Builder.class)
public class Exhibition {

	private final String title;
	private final Date date;
	private final String content;


	@JsonPOJOBuilder(withPrefix = "") // Removes "set" prefix from builder methods
	public static class Builder {
		private String title = "New Exhibition";
		private Date date = Date.from(Instant.now());
		private String content = "";
	}

//	@JsonCreator
//	public Exhibition(@JsonProperty("title") String title, @JsonProperty("title") Date date, @JsonProperty("title") String content) {
//		this.title = title;
//		this.date = date;
//		this.content = content;
//	}
}
