package es.burl.cms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class ContentManagerApp {

	public static void main(String[] args) {
		log.debug("Application is starting up, get ready");
		SpringApplication.run(ContentManagerApp.class, args);
	}
}
