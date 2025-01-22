package es.burl.cms;

import es.burl.cms.data.Site;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

	@Value("${gallery.root}")
	private String galleryRoot;

	@Bean
	public Site site(){
		if (galleryRoot != null) {
			return Site.getFakeSite(galleryRoot);
		} else {
			// Handle error if uploadDir is not injected correctly
			System.out.println("Error: uploadDir is not initialized!");
		}
		return null;
	}

	@Bean
	public String galleryRoot() {
		return galleryRoot;
	}
}
