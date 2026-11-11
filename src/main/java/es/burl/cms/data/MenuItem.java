package es.burl.cms.data;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Data
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonDeserialize(builder = MenuItem.Builder.class)
@lombok.Builder(builderClassName = "Builder", toBuilder = true)
public class MenuItem {
	@Setter
	private int order;
	private String title;
	private String url;

	@JsonPOJOBuilder(withPrefix = "")
	public static class Builder {
		private int order =  Integer.MAX_VALUE;
		private String title = "";
		private String url = "";
	}
}
