package es.burl.cms.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;

import java.util.Date;

@Data
@Getter
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class Exhibition {

	private final String title;
	private final Date date;
	private final String content;

	@JsonCreator
	public Exhibition(@JsonProperty("title") String title, @JsonProperty("title") Date date, @JsonProperty("title") String content) {
		this.title = title;
		this.date = date;
		this.content = content;
	}
}
