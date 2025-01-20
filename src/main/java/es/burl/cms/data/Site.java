package es.burl.cms.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
@Getter
public class Site {

	private Menu menu;
	private List<Page> pages;

}
