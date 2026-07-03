package com.pms.hotel.health;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.pms.hotel.config.SecurityConfig;

@WebMvcTest(PingController.class)
@Import(SecurityConfig.class)
class PingControllerTest {

	@Autowired
	private MockMvcTester mvc;

	@Test
	void pingReturnsUp() {
		assertThat(mvc.get().uri("/api/ping")).hasStatusOk().bodyJson().extractingPath("$.status").isEqualTo("UP");
	}

}
