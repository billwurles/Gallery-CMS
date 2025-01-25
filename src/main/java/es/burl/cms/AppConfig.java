package es.burl.cms;

import es.burl.cms.backup.JsonBackup;
import es.burl.cms.data.Site;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Configuration
public class AppConfig {

	private Site site;

	@Value("${gallery.root}")
	private String galleryRoot;

	@Value("${backup.path}")
	private Path backupPath;

	@Bean
	public Site getSite(){
		if(site == null){
			if(Files.exists(backupPath)){
				try {
					site = new JsonBackup(backupPath).restore();
				} catch (RuntimeException e){
					log.error("Unable to get Site from JSON backup", e);
				}
			} else {
				site =  Site.getFakeSite(galleryRoot);
			}
		}
		return site;
	}

	@Bean
	public String getGalleryRoot() {
		return galleryRoot;
	}

	@Bean
	public Path getBackupPath() {
		return backupPath;
	}
}
