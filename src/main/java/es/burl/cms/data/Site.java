package es.burl.cms.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Data
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE)
@Builder(builderClassName = "Builder", toBuilder = true)
@JsonDeserialize(builder = Site.Builder.class)
public class Site {

	@Getter
	private final String name;
	private final Map<String, Page> pages;
	private final List<Exhibition> exhibitions;

	@JsonPOJOBuilder(withPrefix = "") // Configure Jackson to handle the builder
	public static class PaintingBuilder {
		// Set default values here
		private String name = "Untitled Site";
		private Map<String, Page> pages = new HashMap<>();
		private List<Exhibition> exhibitions = new ArrayList<>();
	}

//	@JsonCreator
//	public Site(@JsonProperty("name") String name, @JsonProperty("pages") HashMap<String, Page> pages) {
//		this.name = name;
//		this.pages = pages;
//	}

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

	public void addNewPage(Page page) {
		log.debug("Saving page to site: {}, {}, {}, {}", page.getTitle(), page.getUrl(), page.getContent(), page.isShowInMenu());
		pages.put(page.getUrl(), page);
	}

	public void removePage(String url) {
		log.debug("Deleting page from site: {}", pages.get(url).toString());
		pages.remove(url);
	}

	public Gallery getPageGallery(String url) {
		return pages.get(url).getGallery();
	}

	public void addExhibition(Exhibition exhibition) {
		this.exhibitions.add(exhibition);
	}

	public List<Exhibition> getExhibitions() {
		exhibitions.sort(Comparator.comparing(Exhibition::getDate));
		return exhibitions;
	}

	public List<Exhibition> getExhibitionsPage(int page, int postsPerPage) {
		exhibitions.sort(Comparator.comparing(Exhibition::getDate));
		int to = page * postsPerPage;
		int from = to - postsPerPage;
		if (to > exhibitions.size()) to = exhibitions.size();
		return exhibitions.subList(from, to);
	}
}
