package es.burl.cms.data;

import lombok.Data;

@Data
public class Painting {
	private String imagePath;
	private String title;
	private String dimensions;
	private boolean sold = false;
}
