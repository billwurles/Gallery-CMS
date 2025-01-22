package es.burl.cms.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;


@Getter
public class ImageUploadDTO {

	private final List<ImageData> images;

	@JsonCreator
	public ImageUploadDTO(@JsonProperty("images") List<ImageData> images){
		this.images = images;
	}

	public record ImageData(String title, String filename, String dimensions, boolean sold, int order, String imageData) {
		@JsonCreator
		public ImageData(@JsonProperty("title") String title,
						 @JsonProperty("filename") String filename,
						 @JsonProperty("dimensions") String dimensions,
						 @JsonProperty("sold") boolean sold,
						 @JsonProperty("order") int order,
						 @JsonProperty("imageData") String imageData) {
			this.title = title;
			this.filename = filename;
			this.dimensions = dimensions;
			this.sold = sold;
			this.order = order;
			this.imageData = imageData;
		}
		}
}

