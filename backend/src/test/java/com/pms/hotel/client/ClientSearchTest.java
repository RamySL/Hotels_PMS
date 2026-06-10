package com.pms.hotel.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ClientSearchTest {

	@Test
	void normalizationIgnoresCaseAccentsAndExtraSpaces() {
		assertThat(ClientSearch.normalize("  Hélène   LEFÈVRE ")).isEqualTo("helene lefevre");
	}

	@Test
	void searchTextCombinesNamesEmailAndPhoneDigits() {
		assertThat(ClientSearch.searchText("Hélène", "Lefèvre", "helene@mail.fr", "06 12.34-56 78"))
			.isEqualTo("helene lefevre helene@mail.fr 0612345678");
	}

	@Test
	void searchTextOfWalkInClientHasOnlyNames() {
		assertThat(ClientSearch.searchText("Jean", "Dupont", null, null)).isEqualTo("jean dupont");
	}

	@ParameterizedTest
	@CsvSource({ "06 12, 0612", "06.12.34, 061234", "+33 6, 336", "(0)6-12, 0612" })
	void phoneLikeTermKeepsOnlyDigits(String term, String key) {
		assertThat(ClientSearch.searchKey(term)).isEqualTo(key);
	}

	@ParameterizedTest
	@CsvSource({ "Hélène, helene", "LEFÈVRE, lefevre", "jean-pierre, jean-pierre", "dupont 06, dupont 06" })
	void otherTermsAreNormalized(String term, String key) {
		assertThat(ClientSearch.searchKey(term)).isEqualTo(key);
	}

}
