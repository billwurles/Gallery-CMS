package es.burl.cms.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.*;
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
				.map(page -> new MenuItem(page.getTitle(), "/page/"+page.getUrl()))
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

	public void addNewPage(String title, String url, String content, boolean showInMenu, Gallery gallery){
		log.debug("Saving page to site: {}, {}, {}, {}", title,url,content,showInMenu);
		pages.put(url, new Page(title, url, pages.size()+1, content, showInMenu, gallery));
	}

	public void removePage(String url){
		log.debug("Deleting page from site: {}", pages.get(url).toString());
		pages.remove(url);
	}

	public Gallery getPageGallery(String url) {
		return pages.get(url).getGallery();
	}















	public static Site getFakeSite(String galleryDir){ //TODO: make a test class
		HashMap<String, Page> pages = new HashMap<>();
		for(int i=1; i<3; i++){
			String url = "pageurl"+i;
			pages.put(url,
					new Page("pageTitle"+i, url, i, "some content"+i, true, new Gallery(new ArrayList<>()))
			);
		}
		pages.put("the-sea", new Page("The Sea","the-sea",3,"",true, getImageFiles(galleryDir)));
//		pages.put("otherulr", new Page("pagetitlenoshow","otherulr",-1,"more content",false, null));
		return new Site("Editor CMS thing", pages);
	}

	public static Gallery getImageFiles(String galleryDir) {
		// Locate the folder in the static/images directory
//		File imageDir = null;
//		try {
//			imageDir = new ClassPathResource("static/images/extracted-ahc-images/thesea").getFile();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
		// Locate the folder in the uploadDir folder instead of resources
		File imageDir = new File(galleryDir, "the-sea");

		// Check if the directory exists, create it if necessary
		if (!imageDir.exists()) {
			log.debug("Directory {} does not exist, creating",imageDir);
			if (!imageDir.mkdirs()) {
				// Handle error if directory cannot be created
				log.error("Failed to create directory: " + imageDir.getAbsolutePath());
			}
		}

		// List to hold ImageFile objects
		Map<String, Painting> imageFiles = new HashMap<>();

		boolean sold = true;
		// Traverse the directory
		int i = 0;
		for (File file : imageDir.listFiles()) {
//			log.debug("Traversing dir for file: {}",file);
			// Ensure it's a file and ends with .jpg
			if (file.isFile() && file.getName().endsWith(".jpg")) {
//				log.debug("Found jpg - {}", file.getName());
				// Extract filename without extension
				String fileNameWithoutExt = file.getName().replace(".jpg", "");

				// Add the Painting object to the list
				sold = !sold;
				imageFiles.put(fileNameWithoutExt, new Painting(fileNameWithoutExt, file.getName(), "10x10", sold, i++));
			}
		}

		return new Gallery(imageFiles);
	}

}
