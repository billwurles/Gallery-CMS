package es.burl.cms.builder;

import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.context.IWebContext;
import org.thymeleaf.linkbuilder.StandardLinkBuilder;

import java.util.Map;

/**
 * Builds links for the published static site, where there is no request to take a context path from.
 * Links written as {@code @{/...}} get the configured base path, so the site can be served under a
 * path prefix (e.g. {@code /gallery-cms} behind the reverse proxy) as well as at the root.
 */
class ServerlessLinkBuilder extends StandardLinkBuilder {

	private final String basePath;

	/** Context variable that overrides the base path for one build. */
	static final String LINK_BASE = "linkBase";

	ServerlessLinkBuilder() {
		this("/");
	}

	ServerlessLinkBuilder(String basePath) {
		this.basePath = normalise(basePath);
	}

	/** "" or "/" -> "/", "gallery-cms/" -> "/gallery-cms". */
	static String normalise(String basePath) {
		String path = basePath == null ? "" : basePath.trim();
		while (path.endsWith("/")) path = path.substring(0, path.length() - 1);
		if (path.isEmpty()) return "/";
		return path.startsWith("/") ? path : "/" + path;
	}

	String getBasePath() {
		return basePath;
	}

	@Override
	protected String computeContextPath(final IExpressionContext context,
										final String base,
										final Map<String, Object> parameters) {

		if (context instanceof IWebContext) {
			return super.computeContextPath(context, base, parameters);
		}
		// A build can set its own base (the editor's preview lives under /preview); otherwise the published one
		Object linkBase = context.getVariable(LINK_BASE);
		return linkBase != null ? linkBase.toString() : basePath;
	}
}
