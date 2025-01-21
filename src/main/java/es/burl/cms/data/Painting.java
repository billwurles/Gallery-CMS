package es.burl.cms.data;

import lombok.Data;

@Data
public class Painting {
	private final String title;
	private final String imagePath;
	private String dimensions;
	private boolean sold = false;
}
