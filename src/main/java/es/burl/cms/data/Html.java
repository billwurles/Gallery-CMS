package es.burl.cms.data;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

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

	/** Links: web, email and phone links, or links within the site. Never javascript: or data:. */
	private static final Pattern LINK = Pattern.compile("(?i)^\\s*(https?:|mailto:|tel:|/|#|[^:/?#]*(?:[/?#]|$)).*", Pattern.DOTALL);
	/** Images: web addresses, the site's own paths, or the editor's inline pictures (data images only). */
	private static final Pattern IMAGE = Pattern.compile("(?i)^\\s*(https?:|/|data:image/(png|jpe?g|gif|webp);base64,|[^:/?#]*(?:[/?#]|$)).*", Pattern.DOTALL);
	/** Embedded video: the YouTube and Vimeo players the editor's video button inserts. */
	private static final Pattern VIDEO = Pattern.compile("(?i)^\\s*(https?:)?//(www\\.)?(youtube\\.com/embed/|youtube-nocookie\\.com/embed/|player\\.vimeo\\.com/video/)[\\w\\-?=&./%]*$");

	/**
	 * What page and exhibition text may contain: the formatting, links, images, tables and video embeds the
	 * rich text editor produces, with its style attributes. Scripts, event handlers, forms, other iframes and
	 * javascript: links are removed. Built from what the live content uses (CV and exhibitions).
	 */
	private static final PolicyFactory POLICY = new HtmlPolicyBuilder()
			.allowElements("p", "br", "div", "span", "font", "b", "strong", "i", "em", "u", "s", "strike", "del", "ins",
					"sub", "sup", "small", "big", "mark", "h1", "h2", "h3", "h4", "h5", "h6", "blockquote", "pre", "code",
					"ul", "ol", "li", "dl", "dt", "dd", "hr", "a", "img", "figure", "figcaption",
					"table", "thead", "tbody", "tfoot", "tr", "td", "th", "caption", "colgroup", "col", "iframe")
			.allowAttributes("class", "lang", "dir", "title", "align").globally()
			.allowStyling()
			.allowAttributes("color", "face", "size").onElements("font")
			.allowUrlProtocols("http", "https", "mailto", "tel", "data")
			.allowAttributes("href").matching(LINK).onElements("a")
			.allowAttributes("target").matching(Pattern.compile("_blank|_self")).onElements("a")
			.allowAttributes("name").onElements("a")
			.allowAttributes("src").matching(IMAGE).onElements("img")
			.allowAttributes("alt", "width", "height").onElements("img")
			.allowAttributes("src").matching(VIDEO).onElements("iframe")
			.allowAttributes("width", "height", "frameborder", "allowfullscreen").onElements("iframe")
			.allowAttributes("width", "border", "cellpadding", "cellspacing").onElements("table")
			.allowAttributes("colspan", "rowspan", "width", "height", "valign").onElements("td", "th")
			.allowAttributes("span", "width").onElements("col", "colgroup")
			.allowAttributes("start", "type").onElements("ol")
			.toFactory();

	/**
	 * The content made safe to publish: the stray editor field removed, then everything the policy above
	 * doesn't allow. Pages and exhibitions run every piece of content through this as they're built.
	 */
	public static String clean(String html) {
		if (html == null) return null;
		return POLICY.sanitize(EDITOR_FIELD.matcher(html).replaceAll(""));
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
