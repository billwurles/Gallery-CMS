package es.burl.cms.builder;

import es.burl.cms.data.Site;

import java.io.IOException;

public interface SiteBuilder {

	public void buildSite(Site site) throws IOException;
}
