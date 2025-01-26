package es.burl.cms.backup;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.burl.cms.data.Site;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Data
@RequiredArgsConstructor
public class JsonBackup implements BackupSite {

	private final Path backupPath;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false); // Allow unknown properties

	@Override
	public void backup(Site site) {
		try {
            // Serialize Site object to JSON
            String jsonSite = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(site);
			try {
				log.debug("Backing up site to: {} ", site);
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
}
