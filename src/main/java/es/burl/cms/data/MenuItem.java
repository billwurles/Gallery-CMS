package es.burl.cms.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
public class MenuItem {
	@Setter
	private int order;
	private String name;
	private String url;
}
