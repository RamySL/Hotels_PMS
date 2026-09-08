package com.pms.hotel;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;

import com.pms.hotel.demo.DemoData;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class PmsApplicationTests {

	@Autowired(required = false)
	private Flyway flyway;

	@Autowired
	private ApplicationContext context;

	@Test
	void contextLoads() {
	}

	@Test
	void flywayIsAutoConfigured() {
		assertThat(flyway).as("Flyway doit être auto-configuré (spring-boot-starter-flyway)").isNotNull();
	}

	/** Les données de démo n'existent que dans docker-compose, avec le profil demo. */
	@Test
	void createsNoDemoDataWithoutTheDemoProfile() {
		assertThat(context.getBeanNamesForType(DemoData.class)).isEmpty();
	}

}
