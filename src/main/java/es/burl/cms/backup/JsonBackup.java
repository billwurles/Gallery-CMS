package es.burl.cms.backup;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import es.burl.cms.data.Page;
import es.burl.cms.data.Painting;
import es.burl.cms.data.Site;
import es.burl.cms.helper.Filesystem;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;

import static com.fasterxml.jackson.databind.jsonFormatVisitors.JsonValueFormat.UUID;

@Slf4j
@Data
@RequiredArgsConstructor
public class JsonBackup implements BackupSite {

	private final Path backupPath;

	//TODO: Consider using NoSQL

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false); // Allow unknown properties in the json

	@Override
	public void backup(Site site) {
		try {
            // Serialize Site object to JSON
			log.debug("Backing up to JSON");
            String jsonSite = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(site);
			try {
				Files.createDirectories(backupPath.getParent());
				Files.writeString(backupPath, jsonSite);

			} catch (IOException e) {
				log.error("Failed to backup site: {} - {}", site.getName(), e);
				throw new RuntimeException(e);
			}
		} catch (JsonProcessingException e) {
			log.error("Failed to convert site to JSON: {} - {}", site.getName(), e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public Site restore() {
		try {
            // Read JSON content from backup
			String json = Files.readString(backupPath);
            return objectMapper.readValue(json, Site.class);
		} catch (JsonProcessingException e) {
			log.error("Failed to parse JSON: - {}", e);
			throw new RuntimeException(e);

		} catch (IOException e) {
			log.error("Failed to read site from backup: - {}", e);
			throw new RuntimeException(e);
		}
	}

	public static void main(String[] args) throws IOException {
		ObjectMapper objectMapper = new ObjectMapper()
			.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		String json = Files.readString(Path.of("appdata/cms/backup/definitely_live/site-data-backup.json"));
		Site site = objectMapper.readValue(json, Site.class);
		for(Page page : site.getPages().values()){
			copyGalleryImagesToNewUrl(page, page.getUrl(), Path.of("appdata/cms/gallery.old"),Path.of("appdata/cms/gallery"));
		}
		JsonBackup backup = new JsonBackup(Path.of("appdata/cms/backup/site-data-backup.json"));
		backup.backup(site);
	}


	public static boolean copyGalleryImagesToNewUrl(Page page, String url, Path oldGalleryRoot, Path newGallery) {
		// Construct the page path and image directory path, create dirs if they don't exist
		Path newPageGallery = newGallery.resolve(url);
		File oldPageGallery = oldGalleryRoot.resolve(url).toFile();
		if(page.getGallery().isEmpty()) return true;
		try {
			Files.createDirectories(newPageGallery);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		System.out.println("Moving gallery folder from "+oldPageGallery+" to "+ newPageGallery);

		//Copy all the images from a gallery
		for(File file : oldPageGallery.listFiles()) {
			String originalFilename = file.getName();
			String newFilename = Painting.generateFilename(originalFilename);
			Painting p = page.getGallery().getGallery().remove(originalFilename);
			if(p!=null) {
				page.getGallery().addPainting(p.toBuilder().filename(newFilename).build());
				try {
//					Filesystem.copyImage(originalFilename, file, newPageGallery);
//					log.debug("Copying image {}", filename);
					Path imagePath = newPageGallery.resolve(newFilename);
					Files.copy(file.toPath(), imagePath, StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			} else System.out.println("No p"+originalFilename);
		}
		return true; //TODO: add more edges cases and return false
	}
}
