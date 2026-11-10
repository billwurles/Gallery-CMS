package es.burl.cms.data;

import java.util.regex.Pattern;

/** Small checks and clean-ups for the rich text HTML that pages and exhibitions store. */
public final class Html {

	private Html() {}

	private static final Pattern MEDIA = Pattern.compile("<\\s*(img|iframe|video|audio|embed|object|table|hr)\\b", Pattern.CASE_INSENSITIVE);

	/**
	 * Until 2026-11 the editor's hidden content field sat inside the rich text box, so saving a page or
	 * exhibition could store {@code <textarea name="content" ...></textarea>} as part of its content.
	 */
	private static final Pattern EDITOR_FIELD = Pattern.compile("<textarea\\b[^>]*>[\\s\\S]*?</textarea\\s*>", Pattern.CASE_INSENSITIVE);

	/** The content with any stray editor field removed. */
	public static String clean(String html) {
		if (html == null) return null;
		return EDITOR_FIELD.matcher(html).replaceAll("");
	}

	/**
	 * HTML with nothing to see: only tags, line breaks, non-breaking spaces and whitespace. The rich text
	 * editor leaves "<p><br></p>" behind when its text is deleted, and that must not count as text.
	 * Images, video and tables count as something to see.
	 */
	public static boolean isBlank(String html) {
		if (html == null) return true;
		String cleaned = clean(html);
		if (MEDIA.matcher(cleaned).find()) return false;
		String text = cleaned.replaceAll("<[^>]*>", "")
				.replaceAll("(?i)&nbsp;|&#160;|&#xa0;", " ")
				.replaceAll("[\\s\\u00a0\\u200b\\ufeff]+", "");
		return text.isEmpty();
	}
}
