package es.burl.cms.data;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Rules for page and exhibition addresses. They become folder names in the gallery and the published site,
 * so they must be plain: lowercase letters, numbers and single hyphens, which also rules out "..", "/" and
 * anything else that could reach outside those folders.
 */
public final class Slugs {

	private Slugs() {}

	private static final Pattern SLUG = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");
	static final int MAX_LENGTH = 100;

	/** Folders at the top of the published site that a page can't take over. */
	public static final Set<String> RESERVED_FOR_PAGES = Set.of("exhibitions", "include", "images");

	/** What's wrong with a page address, or null if it's fine. */
	public static String pageProblem(String slug) {
		String problem = problem(slug);
		if (problem != null) return problem;
		if (RESERVED_FOR_PAGES.contains(slug)) return "The address \"" + slug + "\" is used by the site itself; choose another title";
		return null;
	}

	/** What's wrong with an exhibition address, or null if it's fine. */
	public static String exhibitionProblem(String slug) {
		return problem(slug);
	}

	private static String problem(String slug) {
		if (slug == null || slug.isBlank()) return "The address is empty; give it a title";
		if (slug.length() > MAX_LENGTH) return "The address is too long (" + slug.length() + " characters, at most " + MAX_LENGTH + "); shorten the title";
		if (!SLUG.matcher(slug).matches()) return "The address \"" + slug + "\" can only use lowercase letters, numbers and single hyphens";
		return null;
	}
}
