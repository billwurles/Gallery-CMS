package es.burl.cms.importer;

import es.burl.cms.data.Site;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.zip.ZipFile;

@RequiredArgsConstructor
public class ImporterIWeb implements Importer {

	File zippedSite;

	public Site importSite(){
//		ZipFile zip = zippedSite;

		return new Site();
	}

}
