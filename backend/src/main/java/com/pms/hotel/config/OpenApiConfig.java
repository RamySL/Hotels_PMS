package com.pms.hotel.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * Documentation de l'API générée par springdoc : /v3/api-docs (OpenAPI), affichée par
 * Swagger UI sur /swagger-ui.html. Toutes les routes exigent un JWT, sauf celles qui
 * déclarent le contraire (@SecurityRequirements vide).
 */
@Configuration
public class OpenApiConfig {

	private static final String BEARER_JWT = "bearer-jwt";

	@Bean
	OpenAPI openApi() {
		return new OpenAPI()
			.info(new Info().title("PMS Hôtel")
				.version("MVP")
				.description("API de gestion hôtelière : chambres, clients, réservations, facturation et "
						+ "tableau de bord. Pour appeler les routes protégées, se connecter avec "
						+ "POST /api/auth/login, puis coller le jeton reçu (accessToken) dans « Authorize »."))
			.components(new Components().addSecuritySchemes(BEARER_JWT,
					new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
			.addSecurityItem(new SecurityRequirement().addList(BEARER_JWT));
	}

}
