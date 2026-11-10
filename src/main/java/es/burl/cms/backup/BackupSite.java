package es.burl.cms.backup;

import es.burl.cms.data.Site;

public interface BackupSite {

	void backup(Site site);

	Site restore();

	/**
	 * Removes a gallery image file (page "" for the home image) without losing it from older backups.
	 * Returns false if there's no such file or it can't be removed safely.
	 */
	boolean retireImage(String pageUrl, String filename);

}
