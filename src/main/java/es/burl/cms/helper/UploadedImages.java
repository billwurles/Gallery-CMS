package es.burl.cms.helper;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Checks an uploaded image by its bytes, never by the name the browser sent: only JPEG, PNG, GIF and WebP
 * are accepted, and the saved file gets the extension of what the bytes really are. JPEG, PNG and GIF must
 * also decode; the JDK can't decode WebP, so WebP is checked by its header only.
 */
public final class UploadedImages {

	private UploadedImages() {}

	/** An upload that isn't a usable image. The message says which file and why, for the editor to show. */
	public static class InvalidUploadException extends RuntimeException {
		public InvalidUploadException(String message) {
			super(message);
		}
	}

	/** Decodes the base64 data of an upload and checks it; returns the bytes. */
	public static byte[] decode(String base64, String name) {
		if (base64 == null || base64.isBlank()) throw new InvalidUploadException(label(name) + " is empty");
		try {
			return Base64.getDecoder().decode(base64.trim());
		} catch (IllegalArgumentException e) {
			throw new InvalidUploadException(label(name) + " didn't arrive intact; try uploading it again");
		}
	}

	/** The extension for these bytes (".jpg", ".png", ".gif" or ".webp"), or an exception if they aren't an image we take. */
	public static String extensionOf(byte[] bytes, String name) {
		String ext = sniff(bytes);
		if (ext == null) {
			throw new InvalidUploadException(label(name) + " isn't a JPEG, PNG, GIF or WebP image");
		}
		if (!ext.equals(".webp")) {
			BufferedImage image;
			try {
				image = ImageIO.read(new ByteArrayInputStream(bytes));
			} catch (IOException | RuntimeException e) {
				image = null;
			}
			if (image == null) throw new InvalidUploadException(label(name) + " looks like an image but can't be read; it may be damaged");
		}
		return ext;
	}

	/** The image type from the file's first bytes, or null. */
	static String sniff(byte[] b) {
		if (b == null || b.length < 12) return null;
		if ((b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return ".jpg";
		if ((b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G' && b[4] == 0x0D && b[5] == 0x0A) return ".png";
		if (b[0] == 'G' && b[1] == 'I' && b[2] == 'F' && b[3] == '8') return ".gif";
		if (new String(b, 0, 4, StandardCharsets.ISO_8859_1).equals("RIFF") && new String(b, 8, 4, StandardCharsets.ISO_8859_1).equals("WEBP")) return ".webp";
		return null;
	}

	private static String label(String name) {
		return name == null || name.isBlank() ? "An uploaded file" : "\"" + name + "\"";
	}
}
