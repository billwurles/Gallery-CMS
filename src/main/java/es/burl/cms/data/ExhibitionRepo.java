package es.burl.cms.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Data;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE)
@Builder(builderClassName = "Builder", toBuilder = true)
@JsonDeserialize(builder = ExhibitionRepo.Builder.class)
public class ExhibitionRepo {

	private final Map<String, Exhibition> exhibitions;
	private final MenuItem exhibitionOrder;

	@JsonPOJOBuilder(withPrefix = "")
	public static class Builder {
		private Map<String, Exhibition> exhibitions = new HashMap<>();
		private MenuItem exhibitionOrder = new MenuItem(1, "Exhibitions", "exhibitions");
	}

	public void add(Exhibition exhibition){
		exhibitions.put(exhibition.getId(), exhibition);
	}

	public Exhibition get(String id){
		return exhibitions.get(id);
	}

	public List<Exhibition> getExhibitionsInDateOrder() {
		return exhibitions.values()
				.stream()
				.sorted(Comparator.comparing(Exhibition::getDate).reversed())
				.collect(Collectors.toList());
	}

	public List<Exhibition> getExhibitionsPage(int page, int postsPerPage) {
		List<Exhibition> sortedExhibitions = getExhibitionsInDateOrder();
		int to = page * postsPerPage;
		int from = to - postsPerPage;
		if (to > sortedExhibitions.size()) to = sortedExhibitions.size();
		return sortedExhibitions.subList(from, to);
	}

}
