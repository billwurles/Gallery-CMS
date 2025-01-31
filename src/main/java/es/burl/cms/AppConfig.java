package es.burl.cms;

import es.burl.cms.backup.JsonBackup;
import es.burl.cms.data.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

@Slf4j
@Configuration
public class AppConfig {

	private Site site;

	@Value("${gallery.root}")
	private Path galleryRoot;

	@Value("${html.root}")
	private Path htmlRoot;

	@Value("${backup.path}")
	private Path backupPath;

	@Value("${posts.per.page}")
	private int postsPerPage;

	private static final String homepageUniqueKey = "$$@-UniqueHomeKey-@$$";

	@Bean
	public Site getSite() {
		log.debug("Debug Logs Enabled");
		log.info("CMS Application Starting up - getting Site object");

		Path path = Path.of("test");

		try {
			log.debug("path is: "+Files.createDirectory(path).toAbsolutePath().toString());

		} catch (IOException e) {
			throw new RuntimeException(e);
		}


		if (site == null) {
			if (Files.exists(backupPath)) {
				try {
					site = new JsonBackup(backupPath).restore();
				} catch (RuntimeException e) {
					log.error("Unable to get Site from JSON backup", e);
				}
			} else {
				log.debug("No backup available, building fake site");
				site = Site.builder().build();
//				site = getFakeSite(galleryRoot);
			}
		}
		return site;
	}

	@Bean
	@Qualifier("getGalleryRoot")
	public Path getGalleryRoot() {
		return galleryRoot;
	}

	@Bean
	@Qualifier("getBackupPath")
	public Path getBackupPath() {
		return backupPath;
	}

	@Bean
	@Qualifier("getHtmlRoot")
	public Path getHtmlRoot() {
		return htmlRoot;
	}

	@Bean
	@Qualifier("getPostsPerPage")
	public int getPostsPerPage() {
		return postsPerPage;
	}

	@Bean
	@Qualifier("getHomeKey")
	public String getHomeKey() {
		return homepageUniqueKey;
	}

	public static final String loremIpsum = """
			<h2>What is Lorem Ipsum?</h2>
			<p><strong>Lorem Ipsum</strong> is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to make a type specimen book. It has survived not only five centuries, but also the leap into electronic typesetting, remaining essentially unchanged. It was popularised in the 1960s with the release of Letraset sheets containing Lorem Ipsum passages, and more recently with desktop publishing software like Aldus PageMaker including versions of Lorem Ipsum.</p>
			<h2>Why do we use it?</h2>
			<p>It is a long established fact that a reader will be distracted by the readable content of a page when looking at its layout. The point of using Lorem Ipsum is that it has a more-or-less normal distribution of letters, as opposed to using 'Content here, content here', making it look like readable English. Many desktop publishing packages and web page editors now use Lorem Ipsum as their default model text, and a search for 'lorem ipsum' will uncover many web sites still in their infancy. Various versions have evolved over the years, sometimes by accident, sometimes on purpose (injected humour and the like).</p>
			<p><br></p>
			<h2>Where does it come from?</h2>
			<p>Contrary to popular belief, Lorem Ipsum is not simply random text. It has roots in a piece of classical Latin literature from 45 BC, making it over 2000 years old. Richard McClintock, a Latin professor at Hampden-Sydney College in Virginia, looked up one of the more obscure Latin words, consectetur, from a Lorem Ipsum passage, and going through the cites of the word in classical literature, discovered the undoubtable source. Lorem Ipsum comes from sections 1.10.32 and 1.10.33 of "de Finibus Bonorum et Malorum" (The Extremes of Good and Evil) by Cicero, written in 45 BC. This book is a treatise on the theory of ethics, very popular during the Renaissance. The first line of Lorem Ipsum, "Lorem ipsum dolor sit amet..", comes from a line in section 1.10.32.</p>
			<p>The standard chunk of Lorem Ipsum used since the 1500s is reproduced below for those interested. Sections 1.10.32 and 1.10.33 from "de Finibus Bonorum et Malorum" by Cicero are also reproduced in their exact original form, accompanied by English versions from the 1914 translation by H. Rackham.</p>
			""";

	public static Site getFakeSite(Path galleryDir) { //TODO: make a test class
		HashMap<String, Page> pages = new HashMap<>();
//		for (int i = 1; i < 4; i++) {
//			String url = "pageurl" + i;
//			pages.put(url, Page.builder()
//					.menuItem(MenuItem.builder()
//							.title("pageTitle" + i)
//							.url(url)
//							.order(i+1)
//							.build())
//					.content(loremIpsum)
//					.showInMenu(true)
//					.build()
//			);
//		}
		pages.put("flowers", Page.builder()
				.menuItem(MenuItem.builder()
						.title("Flowers")
						.url("flowers")
						.order(5)
						.build())
//				.content(loremIpsum)
				.showInMenu(true)
				.gallery(Gallery.builder()
						.gallery(getImageFiles(galleryDir))
						.build())
				.build()
		);
//		pages.put("new-ting", Page.builder()
//				.menuItem(MenuItem.builder()
//						.title("Big Up")
//						.url("new-ting")
//						.order(6)
//						.build())
//						.content(loremIpsum)
//				.showInMenu(true)
//				.gallery(Gallery.builder()
//						.gallery(getImageFiles(galleryDir))
//						.build())
//				.build()
//		);

//		Map<String, Exhibition> exhibitionList = new HashMap<>();
//		for (int i = 0; i < 30; i++){
////			String id = UUID.randomUUID().toString();
//			exhibitionList.put(String.valueOf(i), Exhibition.builder()
//							.id(String.valueOf(i))
//							.title("Exhibition "+i)
//							.date(Date.from(Instant.now().minusSeconds(i * 1000000L)))
//							.content(loremIpsum)
//							.build()
//			);
//		}

		pages.put(homepageUniqueKey, Page.builder()
						.menuItem(MenuItem.builder()
								.title("Home")
								.url(homepageUniqueKey)
								.build())
//						.content(loremIpsum)
//						.insetImage(pages.get("the-sea").getGallery().getGalleryInOrder().get(0))
						.build()
		);

		Site site = Site.builder()
				.name("Arabella Harcourt-Cooze")
				.pages(pages)
				.exhibitionRepo(ExhibitionRepo.builder()
//						.exhibitions(exhibitionList)
						.build())
				.build();
		log.debug("Got site: {}", site);

		return site;
	}

	public static Map<String, Painting> getImageFiles(Path galleryDir) {
		// Locate the folder in the uploadDir folder instead of resources
		File imageDir = galleryDir.resolve("flowers").toFile();

		// List to hold ImageFile objects
		Map<String, Painting> imageFiles = new HashMap<>();

		boolean sold = false;
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
//				sold = !sold;
				imageFiles.put(file.getName(), Painting.builder()
								.filename(file.getName())
								.title(fileNameWithoutExt)
//								.dimensions("10x10")
								.medium("Oil on Canvas")
								.sold(sold)
								.order(i++)
								.build()
				);
			}
		}
//		log.debug("Got paintings: {}", imageFiles);
		return imageFiles;
	}

}
