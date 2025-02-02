package es.burl.cms.builder;

import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.context.IWebContext;
import org.thymeleaf.linkbuilder.StandardLinkBuilder;

import java.util.Map;

class ServerlessLinkBuilder extends StandardLinkBuilder {

	@Override
	protected String computeContextPath(final IExpressionContext context,
										final String base,
										final Map<String, Object> parameters) {

		if (context instanceof IWebContext) {
			return super.computeContextPath(context, base, parameters);
		}
		//Without this thymeleaf throws errors when building links
		return "/";

	}
}