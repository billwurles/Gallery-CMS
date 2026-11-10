package es.burl.cms.builder;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class ServerlessLinkBuilderTest {

	@ParameterizedTest
	@CsvSource(value = {
			"'',/",
			"/,/",
			"'  ',/",
			"/gallery-cms,/gallery-cms",
			"/gallery-cms/,/gallery-cms",
			"gallery-cms,/gallery-cms",
			"/a/b//,/a/b",
	})
	void normalisesTheBasePath(String input, String expected) {
		assertThat(new ServerlessLinkBuilder(input).getBasePath()).isEqualTo(expected);
	}

	@ParameterizedTest
	@CsvSource(value = {"NULL,/"}, nullValues = "NULL")
	void nullMeansRoot(String input, String expected) {
		assertThat(ServerlessLinkBuilder.normalise(input)).isEqualTo(expected);
	}
}
