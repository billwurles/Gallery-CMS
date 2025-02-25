package es.burl.cms.backup;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.burl.cms.data.Site;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;


@Slf4j
@Data
@RequiredArgsConstructor
public class JsonBackup implements BackupSite {

	private final Path backupPath;
	private final String filename;
	private final int numPublishedBackups;
	private final int numUnpublishedBackups;

	private final static String publishedExt = "-published";
	private final static String unpublishedExt = "-unpublished";
	private final static String fileExt = ".json";

	private LocalDateTime timeSinceLastBackup; //TODO: Don't delete older files within about 1 minute?

	//TODO: Consider using NoSQL

	//TODO: make the unpublished backup an async threaded task to not block gui

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false); // Allow unknown properties in the json

	@Override
	public void backup(Site site) {
		moveOldBackups(false);
		saveToJson(site, false);
	}

	public void publish(Site site){
		moveOldBackups(true);
		saveToJson(site, true);
	}

	private void moveOldBackups(boolean published){
		File backupFolder = backupPath.toFile();
		HashMap<Integer, File> foundFiles = new HashMap<>();
		for(File file : backupFolder.listFiles()) {
			String filename = file.getName();
			if(filename.contains(filename) && filename.contains(published ? publishedExt : unpublishedExt)){
				int num = Integer.parseInt(filename.split("-")[0]);
				foundFiles.put(num, file);
			}
		}

		for(int i = (published ? numPublishedBackups : numUnpublishedBackups); i > 0 ; i--){
			File file = foundFiles.get(i);
			if(file == null) continue;

			if(i+1 > (published ? numPublishedBackups : numUnpublishedBackups)) {
				log.debug("Deleting file {}", file.getName());
				file.delete();
			} else {
				File newName = new File(file.getParent(), buildFilename(i+1, published, file.getName().split("-")[2]));
				log.debug("Moving file {} to {}", file.getName(), newName.getName());
				file.renameTo(newName);
			}
		}
	}

	private void saveToJson(Site site, Boolean published){
		try {
			// Serialize Site object to JSON
			String newFilename = buildFilename(1, published, LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmssyyyyMMdd")));
			log.debug("Backing up to JSON - {}",newFilename);
			String jsonSite = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(site);
			try {
				Files.createDirectories(backupPath.getParent());
				Files.writeString(backupPath.resolve(newFilename), jsonSite);

			} catch (IOException e) {
				log.error("Failed to backup site: {} - {}", site.getName(), e);
				throw new RuntimeException(e);
			}
		} catch (JsonProcessingException e) {
			log.error("Failed to convert site to JSON: {} - {}", site.getName(), e);
			throw new RuntimeException(e);
		}
	}

	public Site buildSiteFromPath(Path path){
		try {
			String json = Files.readString(path);
			return objectMapper.readValue(json, Site.class);

		} catch (JsonProcessingException e) {
			log.error("Failed to parse JSON: - {}", e);
			throw new RuntimeException(e);

		} catch (IOException e) {
			log.error("Failed to read site from backup: - {}", e);
			throw new RuntimeException(e);
		}
	}

	public Site getJsonFromBackup(File backupFolder, boolean published, boolean firstOnly){
		HashMap<Integer, File> foundFiles = new HashMap<>();
		for(File file : backupFolder.listFiles()) {
			String filename = file.getName();
			if (filename.contains(filename) && filename.contains(published ? publishedExt : unpublishedExt)){
				int num = Integer.parseInt(filename.split("-")[0]);
				if(firstOnly && num == 1) {
					return buildSiteFromPath(file.toPath());
				} else {
					foundFiles.put(num, file);
				}
			}
		}
		for(int i = 0; i < (published ? numPublishedBackups : numUnpublishedBackups); i++){
			File file = foundFiles.get(i);
			if(file == null) continue;
			return buildSiteFromPath(file.toPath());
		}
		return null;
	}

	@Override
	public Site restore() {
		// Read JSON content from backup
		File backupFolder = backupPath.toFile();

		log.debug("Getting first unpublished backup");
		Site site = getJsonFromBackup(backupFolder, false, true);

		//Can't find first unpub backup, try get the next best unpublished
		if(site == null){
			log.debug("Can't find first unpublished backup, getting next recent unpublished backup");
			site = getJsonFromBackup(backupFolder, false, false);
		}

		//try get the first published
		if(site == null){
			log.debug("Can't find any unpublished backup, getting first published backup");
			site = getJsonFromBackup(backupFolder, true, true);
		}

		//Can't find first pub backup, try get the next best published
		if(site == null){
			log.debug("Can't find first published backup, getting next recent published backup");
			site = getJsonFromBackup(backupFolder, true, false);
		}

		return site;
	}

	private String buildFilename(int num, boolean published, String dateTime){
		StringBuilder builder = new StringBuilder();
		builder.append(num)
				.append(published ? publishedExt : unpublishedExt)
				.append("-")
				.append(dateTime)
				.append("-")
				.append(filename)
				.append(fileExt);
		return builder.toString();
	}


//	public static void main(String[] args) throws IOException {
//		ObjectMapper objectMapper = new ObjectMapper()
//			.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
//		String json = Files.readString(Path.of("appdata/cms/backup/definitely_live/site-data-backup.json"));
//		Site site = objectMapper.readValue(json, Site.class);
//		for(Page page : site.getPages().values()){
//			copyGalleryImagesToNewUrl(page, page.getUrl(), Path.of("appdata/cms/gallery.old"),Path.of("appdata/cms/gallery"));
//		}
//		JsonBackup backup = new JsonBackup(Path.of("appdata/cms/backup/site-data-backup.json"));
//		backup.backup(site);
//	}
//
//


//	public static boolean copyGalleryImagesToNewUrl(Page page, String url, Path oldGalleryRoot, Path newGallery) {
//		// Construct the page path and image directory path, create dirs if they don't exist
//		Path newPageGallery = newGallery.resolve(url);
//		File oldPageGallery = oldGalleryRoot.resolve(url).toFile();
//		if(page.getGallery().isEmpty()) return true;
//		try {
//			Files.createDirectories(newPageGallery);
//		} catch (IOException e) {
//			throw new RuntimeException(e);
//		}
//
//		System.out.println("Moving gallery folder from "+oldPageGallery+" to "+ newPageGallery);
//
//		//Copy all the images from a gallery
//		for(File file : oldPageGallery.listFiles()) {
//			String originalFilename = file.getName();
//			String newFilename = Painting.generateFilename(originalFilename);
//			Painting p = page.getGallery().getGallery().remove(originalFilename);
//			if(p!=null) {
//				page.getGallery().addPainting(p.toBuilder().filename(newFilename).build());
//				try {
////					Filesystem.copyImage(originalFilename, file, newPageGallery);
////					log.debug("Copying image {}", filename);
//					Path imagePath = newPageGallery.resolve(newFilename);
//					Files.copy(file.toPath(), imagePath, StandardCopyOption.REPLACE_EXISTING);
//				} catch (IOException e) {
//					throw new RuntimeException(e);
//				}
//			} else System.out.println("No p"+originalFilename);
//		}
//		return true; //TODO: add more edges cases and return false
//	}
}
