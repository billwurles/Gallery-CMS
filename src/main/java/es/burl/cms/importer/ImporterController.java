package es.burl.cms.importer;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@Controller
public class ImporterController {

	private static final String UPLOAD_DIR = new File("uploads/").getAbsolutePath() + "/";
	private static final String EXTRACT_DIR = new File("extracted/").getAbsolutePath() + "/";

	@GetMapping("/import")
	public String getImportPage(Model model){
		System.out.println("Working Directory = " + System.getProperty("user.dir"));
		System.out.println("Uploading files to: " + new File(UPLOAD_DIR).getAbsolutePath());
		return "EditorUpload";
	}

	@PostMapping("/import/upload")
	public String handleFileUpload(@RequestParam("file") MultipartFile file, Model model) {
		File uploadDir = new File(UPLOAD_DIR);
		if (!uploadDir.exists()) {
			boolean created = uploadDir.mkdirs();
			System.out.println("Upload directory created: " + created);
		}

		if (file.isEmpty()) {
			model.addAttribute("message", "Please select a file to upload.");
			return "EditorUpload";
		}

		if (file.getSize() > 100 * 1024 * 1024) { // 100MB limit
			model.addAttribute("message", "File size exceeds the limit of 10MB.");
			return "EditorUpload";
		}

		try {
			// Save uploaded file
			String uploadPath = UPLOAD_DIR + file.getOriginalFilename();
			File uploadedFile = new File(uploadPath);
			file.transferTo(uploadedFile);

			// Extract the ZIP file
			String extractedPath = EXTRACT_DIR + uploadedFile.getName().replace(".zip", "");
			new ZipFile(uploadedFile).extractAll(extractedPath);



			model.addAttribute("message", "File uploaded and extracted successfully!");
			model.addAttribute("extractedPath", extractedPath);

		} catch (IOException e) {
			model.addAttribute("message", "Failed to upload and extract file: " + e.getMessage());
		}

		return "EditorUpload";
	}
}
