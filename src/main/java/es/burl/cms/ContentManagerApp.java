package es.burl.cms;

import es.burl.cms.data.Gallery;
import es.burl.cms.data.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.thymeleaf.TemplateEngine;

@Slf4j
@SpringBootApplication
public class ContentManagerApp {

	public static void main(String[] args){
		SpringApplication.run(ContentManagerApp.class, args);
	}
}
