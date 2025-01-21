package es.burl.cms.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@AllArgsConstructor
@Getter
public class MenuItem {
	private String name;
	private String url;
}
