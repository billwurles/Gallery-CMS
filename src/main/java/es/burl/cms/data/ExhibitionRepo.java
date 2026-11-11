package es.burl.cms.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Data;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE)
@lombok.Builder(builderClassName = "Builder", toBuilder = true)
@JsonDeserialize(builder = ExhibitionRepo.Builder.class)
public class ExhibitionRepo {

	private final Map<String, Exhibition> exhibitions;
	private final MenuItem menuItem;

	@JsonPOJOBuilder(withPrefix = "")
	public static class Builder {
		private Map<String, Exhibition> exhibitions = new HashMap<>();
		private MenuItem menuItem = new MenuItem(1, "Exhibitions", "exhibitions");
	}

	public void add(Exhibition exhibition){
		exhibitions.put(exhibition.getUrl(), exhibition);
	}

	public Exhibition get(String id){
		return exhibitions.get(id);
	}

	public int getTotalPages(int postsPerPage){ //TODO: Hold postsperpage in this object, allow editing in the controller
		return (int) Math.ceil((double) exhibitions.size() / postsPerPage);
	}

	public List<Exhibition> getExhibitionsInDateOrder() {
		return exhibitions.values()
				.stream()
				.sorted(Comparator.comparing(Exhibition::getDate).reversed())
				.collect(Collectors.toList());
	}

	public List<Exhibition> getExhibitionsPage(int page, int postsPerPage) {
		List<Exhibition> sortedExhibitions = getExhibitionsInDateOrder();
		if (page < 1) return List.of();
		int from = Math.min((page - 1) * postsPerPage, sortedExhibitions.size()); // past the last page: empty
		int to = Math.min(from + postsPerPage, sortedExhibitions.size());
		return sortedExhibitions.subList(from, to);
	}

}
