package es.burl.cms.helper;

import es.burl.cms.backup.BackupSite;
import es.burl.cms.backup.JsonBackup;
import es.burl.cms.data.Gallery;
import es.burl.cms.data.Page;
import es.burl.cms.data.Painting;
import es.burl.cms.data.Site;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.stream.Collectors;

@Slf4j
public class GroupImages {

	static Path galleryRoot = Path.of("appdata/cms/gallery");

//	public static void main(String[] args) throws IOException {
//		GroupImages g = new GroupImages();
//		g.go();
//	}

//	public void groupByAspectRatio() throws IOException {
//		BackupSite backupSite = new JsonBackup(Path.of("appdata/cms/backup/site-data-backup.json"));
//		Site site = backupSite.restore();
//
//		for(Page page : site.getPagesInOrder()){
//			List<Jpeg> jpegs = new ArrayList<>();
//			for(Painting p : page.getGallery().getGalleryInOrder()){
//				log.error("{} got image {}  - {} - res: {}", page.getMenuItem().getUrl(),p.getFilename());
//				String filename = p.getFilename();
//				File file = galleryRoot.resolve(page.getUrl()).resolve(filename).toFile();
//				if(file.exists()) {
//					int[] resolution = getResolution(file);
//					jpegs.add(new Jpeg(page.getUrl(), p.getFilename(), resolution[0], resolution[1]));
//					log.error("{} got image {}  - {} - res: {}", page.getMenuItem().getUrl(), p.getFilename(), file.exists(), resolution);
//				} else log.error("Not found file {}",p.getFilename());
//			}
//			List<Jpeg> aspectRatio = jpegs.stream().sorted(Comparator.comparingDouble(Jpeg::getAspectRatio)).toList();
//			int order = 0;
//			Map<String, Painting> newPaintings = new HashMap<>();
//			for(Jpeg jpeg : aspectRatio){
//				Painting painting = page.getGallery().getPainting(jpeg.filename);
//				newPaintings.put(painting.getFilename(), painting.toBuilder().order(order++).build());
//			}
//			page.setGallery(page.getGallery().toBuilder().gallery(newPaintings).build());
//		}
//		backupSite.backup(site);
//
//
//	}

	public List<Jpeg> groupByAspectRatio(List<Jpeg> jpegs) {
		return jpegs.stream()
				.sorted()
				.toList();
	}

	public static List<File> findJpegFiles(String folderPath) {


		File folder = new File(folderPath);
		File[] files = folder.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(".jpg") || name.toLowerCase().endsWith(".jpeg");
			}
		});

		List<File> jpegFiles = new ArrayList<>();
		if (files != null) {
			for (File file : files) {
				jpegFiles.add(file);
			}
		}
		return jpegFiles;
	}

	private static int[] getResolution(File file) throws IOException {
			BufferedImage image = ImageIO.read(file);
			return new int[]{image.getWidth(), image.getHeight()};

	}

	class Jpeg {
		private String page;
		private String filename;
		private int width;
		private int height;

		public Jpeg(String page, String filename,  int width, int height) {
			this.page = page;
			this.filename = filename;
			this.width = width;
			this.height = height;
		}

		public double getAspectRatio() {
			return  width / height;
		}

		@Override
		public String toString() {
			return "Jpeg{" + "width=" + width + ", height=" + height + '}';
		}
	}
}