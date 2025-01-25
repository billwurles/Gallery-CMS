package es.burl.cms.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Data
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE)
public class Site {

	@Getter
	private final String name;
	private final HashMap<String, Page> pages;

	@JsonCreator
	public Site(@JsonProperty("name") String name, @JsonProperty("pages") HashMap<String, Page> pages) {
		this.name = name;
		this.pages = pages;
	}

	public Page getPage(String url) {
		return pages.get(url);
	}

	public List<MenuItem> getMenuItems() {
		return pages.values().stream().filter(Page::isShowInMenu).sorted(Comparator.comparingInt(Page::getOrder)).map(page -> new MenuItem(page.getTitle(), "/page/" + page.getUrl())).collect(Collectors.toList());
	}

	public List<Page> getPagesInOrder() {
		return pages.values().stream().sorted(Comparator.comparingInt(Page::getOrder)).collect(Collectors.toList());
	}

	public void updatePageOrder(Map<Integer, String> order) {
		for (Map.Entry<Integer, String> entry : order.entrySet()) {
			int pageId = entry.getKey();
			Page page = pages.get(entry.getValue());
			log.debug("Modifying order {} for {}", pageId, page.getTitle());
			if (page != null) {
				page.setOrder(pageId);
			}
		}
	}

	public void addNewPage(String title, String url, String content, boolean showInMenu, Gallery gallery) {
		log.debug("Saving page to site: {}, {}, {}, {}", title, url, content, showInMenu);
		pages.put(url, new Page(title, url, pages.size() + 1, content, showInMenu, gallery, new ArrayList<>()));
	}

	public void removePage(String url) {
		log.debug("Deleting page from site: {}", pages.get(url).toString());
		pages.remove(url);
	}

	public Gallery getPageGallery(String url) {
		return pages.get(url).getGallery();
	}
}
