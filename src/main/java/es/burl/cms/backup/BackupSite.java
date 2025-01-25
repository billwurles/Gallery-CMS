package es.burl.cms.backup;

import es.burl.cms.data.Site;

public interface BackupSite {

	void backup(Site site);

	Site restore();

}
