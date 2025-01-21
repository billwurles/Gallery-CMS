package es.burl.cms.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class Site {

	@Getter
	private final String name;
	private final HashMap<String, Page> pages;

	public Page getPage(String url){
		return pages.get(url);
	}

	public List<MenuItem> getMenuItems(){
		return pages.values().stream()
				.filter(Page::isShowInMenu)
				.sorted(Comparator.comparingInt(Page::getMenuOrder))
				.map(page -> new MenuItem(page.getTitle(), "/edit/"+page.getUrl()))
				.collect(Collectors.toList());
	}

	public List<Page> getPagesInMenuOrder(){
		return pages.values().stream()
				.sorted(Comparator.comparingInt(Page::getMenuOrder))
				.collect(Collectors.toList());
	}

	public Page findPageByOrderId(int pageId) {
		return pages.values().stream()
				.filter(page -> page.getMenuOrder() == pageId)
				.findFirst()
				.orElse(null);
	}

	public void updatePageOrder(List<Integer> order) {
		for (int i = 0; i < order.size(); i++) {
			Integer pageId = order.get(i);
			Page page = findPageByOrderId(pageId);
			if (page != null) {
				page.setMenuOrder(i + 1);
			}
		}
	}

	public void addNewPage(String title, String url, String content, boolean showInMenu, List<Painting> gallery){
		log.debug("Saving page to site: {}, {}, {}, {}", title,url,content,showInMenu);
		pages.put(url, new Page(title, url, pages.size()+1, content, showInMenu, gallery));
	}

	public void removePage(String url){
		log.debug("Deleting page from site: {}", pages.get(url).toString());
		pages.remove(url);
	}

	public static Site getFakeSite(){ //TODO: make a test class
		HashMap<String, Page> pages = new HashMap<>();
		for(int i=1; i<3; i++){
			String url = "pageurl"+i;
			pages.put(url,
					new Page("pageTitle"+i, url, i, "some content"+i, true, null)
			);
		}
		pages.put("the-sea", new Page("The Sea","the-sea",3,"",true, getImageFiles()));
//		pages.put("otherulr", new Page("pagetitlenoshow","otherulr",-1,"more content",false, null));
		return new Site("Editor CMS thing", pages);
	}

	public static List<Painting> getImageFiles() {
		// Locate the folder in the static/images directory
		File imageDir = null;
		try {
			imageDir = new ClassPathResource("static/images/extracted-ahc-images/thesea").getFile();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// List to hold ImageFile objects
		List<Painting> imageFiles = new ArrayList<>();

		// Traverse the directory
		for (File file : imageDir.listFiles()) {
			// Ensure it's a file and ends with .jpg
			if (file.isFile() && file.getName().endsWith(".jpg")) {
				// Extract filename without extension
				String fileNameWithoutExt = file.getName().replace(".jpg", "");

				String relativePath = "/images/extracted-ahc-images/thesea/" + file.getName();

				// Add the Painting object to the list
				imageFiles.add(new Painting(fileNameWithoutExt, relativePath));
			}
		}

		return imageFiles;
	}

	public List<Painting> getPageGallery(String url) {
		return pages.get(url).getGallery();
	}
}
