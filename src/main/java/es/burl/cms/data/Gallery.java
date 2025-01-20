package es.burl.cms.data;

import lombok.*;

import java.util.List;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
public class Gallery extends Page {

	private List<Painting> paintings;

}
