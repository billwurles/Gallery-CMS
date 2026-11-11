package es.burl.cms.backup;

import es.burl.cms.data.*;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

/**
 * What changes between two states of the site, "from" and "to": pages, paintings, menu order, home image and
 * exhibitions, with a line diff of page and exhibition text. The Versions page uses it with from = the site
 * now and to = a kept version, so it reads as "what restoring this version would change".
 */
public record VersionDiff(List<PageChange> pages, boolean menuReordered, List<String> menuFrom, List<String> menuTo,
						  Change<String> homeImage, List<ExhibitionChange> exhibitions) {

	public enum Kind { ADDED, REMOVED, CHANGED }

	/** One line of a text diff: "+" only in "to", "-" only in "from", " " in both. */
	public record Line(String op, String text) {}

	public record Change<T>(T from, T to) {}

	/** A painting that's only on one side, or whose details differ. {@code side} says where its image is: "from" or "to". */
	public record PaintingChange(String filename, String title, Kind kind, List<String> details, String side) {}

	public record PageChange(String url, String title, Kind kind, List<String> details, List<Line> text,
							 List<PaintingChange> paintings) {}

	public record ExhibitionChange(String url, String title, Kind kind, List<String> details, List<Line> text) {}

	public boolean isEmpty() {
		return pages.isEmpty() && !menuReordered && homeImage == null && exhibitions.isEmpty();
	}

	public static VersionDiff between(Site from, Site to) {
		List<PageChange> pages = new ArrayList<>();
		Set<String> urls = new TreeSet<>(from.getPages().keySet());
		urls.addAll(to.getPages().keySet());
		for (String url : urls) {
			Page a = from.getPages().get(url), b = to.getPages().get(url);
			if (a == null) pages.add(new PageChange(url, b.getTitle(), Kind.ADDED, List.of(), lines(null, b.getContent()), paintings(null, b)));
			else if (b == null) pages.add(new PageChange(url, a.getTitle(), Kind.REMOVED, List.of(), lines(a.getContent(), null), paintings(a, null)));
			else {
				List<String> details = new ArrayList<>();
				if (!Objects.equals(a.getTitle(), b.getTitle())) details.add("Title: “" + a.getTitle() + "” → “" + b.getTitle() + "”");
				if (a.isShowContent() != b.isShowContent()) details.add(b.isShowContent() ? "Text shown" : "Text hidden");
				if (a.isShowGallery() != b.isShowGallery()) details.add(b.isShowGallery() ? "Gallery shown" : "Gallery hidden");
				List<Line> text = lines(a.getContent(), b.getContent());
				boolean textChanged = text.stream().anyMatch(l -> !l.op().equals(" "));
				List<PaintingChange> paintings = paintings(a, b);
				if (!details.isEmpty() || textChanged || !paintings.isEmpty()) {
					pages.add(new PageChange(url, b.getTitle(), Kind.CHANGED, details, textChanged ? text : List.of(), paintings));
				}
			}
		}

		List<String> menuFrom = from.getMenuItems().stream().map(MenuItem::getTitle).toList();
		List<String> menuTo = to.getMenuItems().stream().map(MenuItem::getTitle).toList();
		List<String> sharedFrom = menuFrom.stream().filter(menuTo::contains).toList();
		List<String> sharedTo = menuTo.stream().filter(menuFrom::contains).toList();

		String homeFrom = from.getHomeImage() == null ? "" : from.getHomeImage().getFilename();
		String homeTo = to.getHomeImage() == null ? "" : to.getHomeImage().getFilename();
		Change<String> home = Objects.equals(homeFrom, homeTo) ? null : new Change<>(homeFrom, homeTo);

		List<ExhibitionChange> exhibitions = new ArrayList<>();
		Map<String, Exhibition> ea = from.getExhibitionRepo().getExhibitions(), eb = to.getExhibitionRepo().getExhibitions();
		Set<String> keys = new TreeSet<>(ea.keySet());
		keys.addAll(eb.keySet());
		SimpleDateFormat day = new SimpleDateFormat("d MMM yyyy, HH:mm");
		for (String key : keys) {
			Exhibition a = ea.get(key), b = eb.get(key);
			if (a == null) exhibitions.add(new ExhibitionChange(key, b.getTitle(), Kind.ADDED, List.of(), lines(null, b.getContent())));
			else if (b == null) exhibitions.add(new ExhibitionChange(key, a.getTitle(), Kind.REMOVED, List.of(), lines(a.getContent(), null)));
			else {
				List<String> details = new ArrayList<>();
				if (!Objects.equals(a.getTitle(), b.getTitle())) details.add("Title: “" + a.getTitle() + "” → “" + b.getTitle() + "”");
				if (!Objects.equals(a.getDate(), b.getDate())) {
					details.add("Date: " + (a.getDate() == null ? "none" : day.format(a.getDate())) + " → " + (b.getDate() == null ? "none" : day.format(b.getDate())));
				}
				List<Line> text = lines(a.getContent(), b.getContent());
				boolean textChanged = text.stream().anyMatch(l -> !l.op().equals(" "));
				if (!details.isEmpty() || textChanged) exhibitions.add(new ExhibitionChange(key, b.getTitle(), Kind.CHANGED, details, textChanged ? text : List.of()));
			}
		}
		return new VersionDiff(pages, !sharedFrom.equals(sharedTo), menuFrom, menuTo, home, exhibitions);
	}

	private static List<PaintingChange> paintings(Page a, Page b) {
		Map<String, Painting> pa = a == null || a.getGallery() == null ? Map.of() : a.getGallery().getGallery();
		Map<String, Painting> pb = b == null || b.getGallery() == null ? Map.of() : b.getGallery().getGallery();
		List<PaintingChange> out = new ArrayList<>();
		List<String> orderA = a == null || a.getGallery() == null ? List.of() : a.getGallery().getGalleryInOrder().stream().map(Painting::getFilename).filter(pb::containsKey).toList();
		List<String> orderB = b == null || b.getGallery() == null ? List.of() : b.getGallery().getGalleryInOrder().stream().map(Painting::getFilename).filter(pa::containsKey).toList();
		for (Painting p : pb.values()) {
			Painting old = pa.get(p.getFilename());
			if (old == null) { out.add(new PaintingChange(p.getFilename(), p.getTitle(), Kind.ADDED, List.of(), "to")); continue; }
			List<String> details = new ArrayList<>();
			if (!Objects.equals(old.getTitle(), p.getTitle())) details.add("Title: “" + old.getTitle() + "” → “" + p.getTitle() + "”");
			if (!Objects.equals(old.getDimensions(), p.getDimensions())) details.add("Dimensions: " + blank(old.getDimensions()) + " → " + blank(p.getDimensions()));
			if (!Objects.equals(old.getMedium(), p.getMedium())) details.add("Medium: " + blank(old.getMedium()) + " → " + blank(p.getMedium()));
			if (old.isSold() != p.isSold()) details.add(p.isSold() ? "Marked sold" : "No longer sold");
			int from = orderA.indexOf(p.getFilename()), to = orderB.indexOf(p.getFilename());
			if (from != to) details.add("Position " + (from + 1) + " → " + (to + 1));
			if (!details.isEmpty()) out.add(new PaintingChange(p.getFilename(), p.getTitle(), Kind.CHANGED, details, "to"));
		}
		for (Painting p : pa.values()) {
			if (!pb.containsKey(p.getFilename())) out.add(new PaintingChange(p.getFilename(), p.getTitle(), Kind.REMOVED, List.of(), "from"));
		}
		out.sort(Comparator.comparing(PaintingChange::kind).thenComparing(c -> c.title() == null ? "" : c.title()));
		return out;
	}

	private static String blank(String s) {
		return s == null || s.isBlank() ? "(blank)" : s;
	}

	private static final Pattern BREAKS = Pattern.compile("(?i)<br\\s*/?>|</(p|div|h[1-6]|li|tr|blockquote|pre|table)>");

	/** Page text as plain lines: tags dropped, one line per paragraph, heading, list item or row. */
	static List<String> text(String html) {
		if (html == null || Html.isBlank(html)) return List.of();
		String plain = BREAKS.matcher(Html.clean(html)).replaceAll("\n").replaceAll("<[^>]*>", "")
				.replace("&nbsp;", " ").replace("&#160;", " ").replace("&lt;", "<").replace("&gt;", ">")
				.replace("&quot;", "\"").replace("&#39;", "'").replace("&#61;", "=").replace("&amp;", "&")
				.replace(' ', ' ');
		List<String> lines = new ArrayList<>();
		for (String line : plain.split("\n")) {
			String t = line.strip().replaceAll("\\s+", " ");
			if (!t.isEmpty()) lines.add(t);
		}
		return lines;
	}

	/** A line diff of two pieces of HTML's text (longest common subsequence). */
	static List<Line> lines(String fromHtml, String toHtml) {
		List<String> a = text(fromHtml), b = text(toHtml);
		int[][] lcs = new int[a.size() + 1][b.size() + 1];
		for (int i = a.size() - 1; i >= 0; i--) {
			for (int j = b.size() - 1; j >= 0; j--) {
				lcs[i][j] = a.get(i).equals(b.get(j)) ? lcs[i + 1][j + 1] + 1 : Math.max(lcs[i + 1][j], lcs[i][j + 1]);
			}
		}
		List<Line> out = new ArrayList<>();
		int i = 0, j = 0;
		while (i < a.size() && j < b.size()) {
			if (a.get(i).equals(b.get(j))) { out.add(new Line(" ", a.get(i))); i++; j++; }
			else if (lcs[i + 1][j] >= lcs[i][j + 1]) out.add(new Line("-", a.get(i++)));
			else out.add(new Line("+", b.get(j++)));
		}
		while (i < a.size()) out.add(new Line("-", a.get(i++)));
		while (j < b.size()) out.add(new Line("+", b.get(j++)));
		return out;
	}
}
