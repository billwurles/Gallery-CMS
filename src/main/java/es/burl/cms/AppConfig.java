package es.burl.cms;

import es.burl.cms.backup.BackupSite;
import es.burl.cms.backup.JsonBackup;
import es.burl.cms.data.*;
import jakarta.annotation.PostConstruct;
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

	@Value("${static.root}")
	private String staticRoot;

	@Value("${posts.per.page}")
	private int postsPerPage;


	@Value("${backup.path}")
	private Path backupPath;

	@Value("${backup.filename}")
	private String backupFilename;

	@Value("${num.published.backups}")
	private int publishedBackupsNum;

	@Value("${num.unpublished.backups}")
	private int unpublishedBackupsNum;

	private JsonBackup saveService;

	@Bean
	@Qualifier("getSite")
	public Site getSite() {
		log.debug("Debug Logs Enabled");
		log.info("CMS Application Starting up - getting Site object");
		if (site == null) {
			if (Files.exists(backupPath)) {
				try {
					site = getSaveService().restore();
				} catch (RuntimeException e) {
					log.error("Unable to get Site from JSON backup", e);
				}
			} else {
				log.debug("No backup available, creating new site");
				site = Site.builder().build();
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
	@Qualifier("getHtmlRoot")
	public Path getHtmlRoot() {
		return htmlRoot;
	}

	@Bean
	@Qualifier("getStaticRoot")
	public String getStaticRoot() {
		return staticRoot;
	}

	@Bean
	@Qualifier("getPostsPerPage")
	public int getPostsPerPage() {
		return postsPerPage;
	}

	@Bean
	@Qualifier("saveService")
	public BackupSite getSaveService(){
		if(saveService == null) saveService = new JsonBackup(backupPath, backupFilename, publishedBackupsNum, unpublishedBackupsNum);
		return saveService;
	}

	@Bean
	@Qualifier("getPublishService")
	public JsonBackup getPublishService(){
		if(saveService == null) saveService = new JsonBackup(backupPath, backupFilename, publishedBackupsNum, unpublishedBackupsNum);
		return saveService;
	}
}
