package es.burl.cms.importer;

import es.burl.cms.data.Painting;
import org.jsoup.nodes.Element;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class RSSExtractor {

//	public static List<Painting> parseXmlForPaintings(String filePath) {
//		List<Painting> paintings = new ArrayList<>();
//		try {
//			// Get the XML file from the resources folder
//			File file = new File(RSSExtractor.class.getClassLoader().getResource(filePath).toURI());
//			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
//			DocumentBuilder builder = factory.newDocumentBuilder();
//			Document doc = builder.parse(file);
//
//			// Normalize the XML document
//			doc.getDocumentElement().normalize();
//
//			// Get all "item" elements from the RSS feed
//			NodeList items = doc.getElementsByTagName("item");
//
//			// Iterate through each "item" element
//			for (int i = 0; i < items.getLength(); i++) {
//				Node node = items.item(i);
//				if (node.getNodeType() == Node.ELEMENT_NODE) {
//					Element itemElement = (Element) node;
//
//					// Extract the title (text inside <title>)
//					String title = itemElement.getElementsByTag("title").get(0).getTextContent();
//
//					// Extract the image URL (inside <enclosure>)
//					String imageUrl = itemElement.getElementsByTag("enclosure").get(0).getAttributes().getNamedItem("url").getTextContent();
//
//					// Extract dimensions if present in the title (e.g., "60x60cm")
//					String dimensions = null;
//					String titleWithoutDimensions = title;
//					if (title.matches(".*\\d+x\\d+.*")) { // Check if dimensions are present
//						dimensions = title.replaceAll(".*?(\\d+x\\d+).*", "$1");
//						titleWithoutDimensions = title.replaceAll("\\s*\\d+x\\d+\\s*", "").trim();
//					}
//
//					// Create Painting object and add it to the list
//					Painting painting = new Painting(imageUrl, titleWithoutDimensions, dimensions);
//					paintings.add(painting);
//				}
//			}
//
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		return paintings;
//	}

//	public static void main(String[] args) {
//		// Example usage - replace with your local HTML file path
//		String filePath = "path/to/your/The_Sea.html";
//		List<Painting> paintings = parseHtmlForImages(filePath);
//
//		// Print results
//		for (Painting painting : paintings) {
//			System.out.println(painting);
//		}
//	}
}