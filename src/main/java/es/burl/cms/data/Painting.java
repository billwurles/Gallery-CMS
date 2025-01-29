package es.burl.cms.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Data;

import java.text.Normalizer;

@Data
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE)
@Builder(builderClassName = "Builder", toBuilder = true)
@JsonDeserialize(builder = Painting.Builder.class)
public class Painting {
	private final String title;
	private final String filename;
	private final String dimensions;
	private final String medium;
	private final boolean sold;// = false;
	private final int order;

	@JsonPOJOBuilder(withPrefix = "") // Removes "set" prefix from builder methods
	public static class Builder {
		private String title = "";
		private String filename = "";
		private String dimensions = "";
		private String medium = "";
		private boolean sold = false;
		private int order = 100;
	}

	public static String generateSafeFilename(String titleToGenerateFrom, String oldFilenameWithExtension) {
		// Step 1: Normalize the string and remove accents
		String normalized = Normalizer.normalize(titleToGenerateFrom, Normalizer.Form.NFD).replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");

		// Step 2: Replace spaces, colons, and other unsafe characters with hyphens
		String sanitized = normalized.replaceAll("[^a-zA-Z0-9\\s]", "") // Remove non-alphanumeric except spaces
				.replaceAll("\\s+", "-")          // Replace spaces with hyphens
				.toLowerCase();                   // Convert to lowercase

		// Step 3: Ensure the filename is not empty and truncate if necessary
		if (sanitized.isEmpty()) {
			sanitized = "default-filename";
		}

		// Limit filename length to 100 characters for safety
		if (sanitized.length() > 100) {
			sanitized = sanitized.substring(0, 100);
		}

		String extension = oldFilenameWithExtension.substring(oldFilenameWithExtension.lastIndexOf("."));

		return sanitized + extension;
	}
}
