package com.pms.hotel.client;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Forme de recherche d'un client : minuscules et sans accents, téléphone réduit à ses
 * chiffres. « Hélène » et « HELENE » donnent « helene », « 06 12 34 » donne « 061234 ».
 */
final class ClientSearch {

	/** Au moins un chiffre, et rien d'autre que des chiffres ou des séparateurs de téléphone. */
	private static final Pattern PHONE_LIKE = Pattern.compile("[0-9 .+()-]*[0-9][0-9 .+()-]*");

	private static final Pattern DIACRITICS = Pattern.compile("\\p{M}");

	private static final Pattern WHITESPACE = Pattern.compile("\\s+");

	private static final Pattern NON_DIGITS = Pattern.compile("[^0-9]");

	private ClientSearch() {
	}

	/** Texte stocké dans search_text. */
	static String searchText(String firstName, String lastName, String email, String phone) {
		return Stream.of(normalize(firstName), normalize(lastName), email == null ? "" : normalize(email),
				phone == null ? "" : digits(phone))
			.filter(part -> !part.isEmpty())
			.collect(Collectors.joining(" "));
	}

	/** Ce qu'il faut chercher dans search_text pour le terme saisi. */
	static String searchKey(String term) {
		return PHONE_LIKE.matcher(term.trim()).matches() ? digits(term) : normalize(term);
	}

	static String normalize(String text) {
		String withoutAccents = DIACRITICS.matcher(Normalizer.normalize(text, Normalizer.Form.NFD)).replaceAll("");
		return WHITESPACE.matcher(withoutAccents.trim()).replaceAll(" ").toLowerCase(Locale.ROOT);
	}

	private static String digits(String text) {
		return NON_DIGITS.matcher(text).replaceAll("");
	}

}
