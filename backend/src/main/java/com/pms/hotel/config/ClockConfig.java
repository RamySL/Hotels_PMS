package com.pms.hotel.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfig {

	/**
	 * Horloge sur le fuseau de l'hôtel : les règles de date (arrivée, check-in) portent
	 * sur sa journée. L'injecter, plutôt qu'appeler LocalDate.now(), rend aussi ces
	 * règles testables.
	 */
	@Bean
	Clock clock(@Value("${pms.time-zone}") String timeZone) {
		return Clock.system(ZoneId.of(timeZone));
	}

}
