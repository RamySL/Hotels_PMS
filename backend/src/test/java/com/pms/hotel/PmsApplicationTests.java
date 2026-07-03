package com.pms.hotel;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class PmsApplicationTests {

	@Autowired(required = false)
	private Flyway flyway;

	@Test
	void contextLoads() {
	}

	@Test
	void flywayIsAutoConfigured() {
		assertThat(flyway).as("Flyway doit être auto-configuré (spring-boot-starter-flyway)").isNotNull();
	}

}
