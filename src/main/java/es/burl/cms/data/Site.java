package es.burl.cms.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Data
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE)
@Builder(builderClassName = "Builder")
@JsonDeserialize(builder = Site.Builder.class)
public class Site {

	@Getter
	private final String name;
	private final ExhibitionRepo exhibitionRepo;
	private final Map<String, Page> pages;

	@JsonPOJOBuilder(withPrefix = "")
	public static class Builder {
		private String name = "Untitled Site";
		private ExhibitionRepo exhibitionRepo = ExhibitionRepo.builder().build();
		private Map<String, Page> pages = new HashMap<>();
	}

	public Page getPage(String url) {
		return pages.get(url);
	}

	public List<MenuItem> getMenuItems() {
		Stream<MenuItem> menuStream = pages.values().stream()
				.filter(Page::isShowInMenu)
				.map(Page::getMenuItem);
		return Stream.concat(menuStream, Stream.of(exhibitionRepo.getMenuItem()))
				.sorted(Comparator.comparingInt(MenuItem::getOrder))
				.collect(Collectors.toList());
	}

	public List<Page> getPagesInOrder() {
		return pages.values()
				.stream()
				.sorted(Comparator.comparingInt(page -> page.getMenuItem().getOrder()))
				.collect(Collectors.toList());
	}

	public void updatePageOrder(Map<Integer, String> order) {
		for (Map.Entry<Integer, String> entry : order.entrySet()) {
			int pageId = entry.getKey();
			if(entry.getValue().equals(exhibitionRepo.getMenuItem().getUrl())){
				exhibitionRepo.getMenuItem().setOrder(pageId);
			} else {
				Page page = pages.get(entry.getValue());
				log.debug("Modifying order {} for {}", pageId, page.getMenuItem().getTitle());
				page.getMenuItem().setOrder(pageId);
			}
		}
	}

	public int getNextPageOrder() {
		return 1 + getMenuItems().stream()
				.filter(menuItem -> menuItem.getOrder() != Integer.MAX_VALUE)
				.max(Comparator.comparingInt(MenuItem::getOrder))
				.map(MenuItem::getOrder)
				.orElse(0);
	}

	public void addNewPage(Page page) {
		log.debug("Saving page to site: {}, {}, {}, {}", page.getMenuItem().getTitle(), page.getMenuItem().getUrl(), page.getContent(), page.isShowInMenu());
		pages.put(page.getMenuItem().getUrl(), page);
	}

	public void removePage(String url) {
		log.debug("Deleting page from site: {}", pages.get(url).toString());
		pages.remove(url);
	}

	public Gallery getPageGallery(String url) {
		return pages.get(url).getGallery();
	}

	public ExhibitionRepo getExhibitionRepo(String id){
		return exhibitionRepo;
	}
}
