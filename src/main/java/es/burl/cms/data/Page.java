package es.burl.cms.data;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Page {

	private String name;
	private String contentHTML;
	private boolean showInMenu = true;

}
