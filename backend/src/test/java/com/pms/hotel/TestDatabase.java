package com.pms.hotel;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Remise à vide de la base entre deux tests d'intégration, qui ne sont pas transactionnels
 * (voir le §6.3 du doc). Les comptes du seed V2 sont conservés.
 */
public final class TestDatabase {

	private TestDatabase() {
	}

	/** Supprime les données métier dans l'ordre des clés étrangères. */
	public static void deleteBusinessData(JdbcTemplate jdbcTemplate) {
		jdbcTemplate.update("DELETE FROM payments");
		jdbcTemplate.update("DELETE FROM invoices");
		jdbcTemplate.update("DELETE FROM reservations");
		jdbcTemplate.update("DELETE FROM clients");
		jdbcTemplate.update("DELETE FROM rooms");
		jdbcTemplate.update("DELETE FROM room_types");
	}

}
