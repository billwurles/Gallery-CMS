package es.burl.cms.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@ToString
@Getter
public class ImageUploadDTO {

	private final List<ImageData> images;

	@JsonCreator
	public ImageUploadDTO(@JsonProperty("images") List<ImageData> images){
		this.images = images;
	}

	@Getter
	@ToString
	public static class ImageData {

		private final String title;
		private final String filename;
		private final String dimensions;
		private final String medium;
		private final boolean sold;
		private final int order;
		private final String imageData;

		@JsonCreator
		public ImageData(@JsonProperty("title") String title,
						 @JsonProperty("filename") String filename,
						 @JsonProperty("dimensions") String dimensions,
						 @JsonProperty("medium") String medium,
						 @JsonProperty("sold") boolean sold,
						 @JsonProperty("order") int order,
						 @JsonProperty("imageData") String imageData) {
			this.title = title;
			this.filename = filename;
			this.dimensions = dimensions;
			this.medium = medium;
			this.sold = sold;
			this.order = order;
			this.imageData = imageData;
		}
		}
}

