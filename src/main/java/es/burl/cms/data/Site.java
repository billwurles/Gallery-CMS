package es.burl.cms.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
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
	public Site(@JsonProperty("name") String name,
				@JsonProperty("pages") HashMap<String, Page> pages) {
		this.name = name;
		this.pages = pages;
	}

	public Page getPage(String url){
		return pages.get(url);
	}

	public List<MenuItem> getMenuItems(){
		return pages.values().stream()
				.filter(Page::isShowInMenu)
				.sorted(Comparator.comparingInt(Page::getOrder))
				.map(page -> new MenuItem(page.getTitle(), "/page/"+page.getUrl()))
				.collect(Collectors.toList());
	}

	public List<Page> getPagesInOrder(){
		List<Page> order= pages.values().stream()
				.sorted(Comparator.comparingInt(Page::getOrder))
				.collect(Collectors.toList());
		for(Page p : order) { log.debug("Got menu order: {} for {}", p.getOrder(), p.getTitle());};
		return order;
	}

	public void updatePageOrder(Map<Integer, String> order) {
		for (Map.Entry<Integer, String> entry : order.entrySet()) {
			int pageId = entry.getKey();
			Page page = pages.get(entry.getValue());
			log.debug("Modifying order {}-{} for {}", pageId, page.getTitle());
			if (page != null) {
				page.setOrder(pageId);
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













	public static String loremIpsum = """
<h2>What is Lorem Ipsum?</h2>
<p><strong>Lorem Ipsum</strong> is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to make a type specimen book. It has survived not only five centuries, but also the leap into electronic typesetting, remaining essentially unchanged. It was popularised in the 1960s with the release of Letraset sheets containing Lorem Ipsum passages, and more recently with desktop publishing software like Aldus PageMaker including versions of Lorem Ipsum.</p>
<h2>Why do we use it?</h2>
<p>It is a long established fact that a reader will be distracted by the readable content of a page when looking at its layout. The point of using Lorem Ipsum is that it has a more-or-less normal distribution of letters, as opposed to using 'Content here, content here', making it look like readable English. Many desktop publishing packages and web page editors now use Lorem Ipsum as their default model text, and a search for 'lorem ipsum' will uncover many web sites still in their infancy. Various versions have evolved over the years, sometimes by accident, sometimes on purpose (injected humour and the like).</p>
<p><br></p>
<h2>Where does it come from?</h2>
<p>Contrary to popular belief, Lorem Ipsum is not simply random text. It has roots in a piece of classical Latin literature from 45 BC, making it over 2000 years old. Richard McClintock, a Latin professor at Hampden-Sydney College in Virginia, looked up one of the more obscure Latin words, consectetur, from a Lorem Ipsum passage, and going through the cites of the word in classical literature, discovered the undoubtable source. Lorem Ipsum comes from sections 1.10.32 and 1.10.33 of "de Finibus Bonorum et Malorum" (The Extremes of Good and Evil) by Cicero, written in 45 BC. This book is a treatise on the theory of ethics, very popular during the Renaissance. The first line of Lorem Ipsum, "Lorem ipsum dolor sit amet..", comes from a line in section 1.10.32.</p>
<p>The standard chunk of Lorem Ipsum used since the 1500s is reproduced below for those interested. Sections 1.10.32 and 1.10.33 from "de Finibus Bonorum et Malorum" by Cicero are also reproduced in their exact original form, accompanied by English versions from the 1914 translation by H. Rackham.</p>
			""";

	public static Site getFakeSite(String galleryDir){ //TODO: make a test class
		HashMap<String, Page> pages = new HashMap<>();
		for(int i=0; i<3; i++){
			String url = "pageurl"+i;
			pages.put(
					url, new Page(
						"pageTitle"+i,
						url,
						i,
						loremIpsum,
						true,
						new Gallery(new HashMap<>())
					)
			);
		}
		pages.put(
				"the-sea", new Page(
						"The Sea",
						"the-sea",
						3,
						"",
						true,
						new Gallery(getImageFiles(galleryDir))
				)
		);
//		pages.put("otherulr", new Page("pagetitlenoshow","otherulr",-1,"more content",false, null));
		return new Site("Editor CMS", pages);
	}

	public static Map<String, Painting> getImageFiles(String galleryDir) {
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
				imageFiles.put(fileNameWithoutExt, new Painting(fileNameWithoutExt, file.getName(), "10x10", "oil on canvas", sold, i++));
			}
		}

		return imageFiles;
	}

}
