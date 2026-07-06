package com.pms.hotel.health;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Santé")
public class PingController {

	@GetMapping("/api/ping")
	@Operation(summary = "Vérifier que l'API répond")
	@SecurityRequirements
	public PingResponse ping() {
		return new PingResponse("UP");
	}

	public record PingResponse(String status) {
	}

}
