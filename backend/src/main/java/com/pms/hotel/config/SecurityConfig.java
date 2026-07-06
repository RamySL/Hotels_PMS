package com.pms.hotel.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.HandlerExceptionResolver;

import com.pms.hotel.auth.JwtProperties;
import com.pms.hotel.auth.JwtService;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http,
			@Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) {
		// Les erreurs d'authentification levées dans les filtres passent par le
		// GlobalExceptionHandler, comme celles des contrôleurs : un seul format RFC 7807.
		AuthenticationEntryPoint entryPoint = (request, response, exception) -> exceptionResolver
			.resolveException(request, response, null, exception);

		http
			// API stateless authentifiée par jeton Bearer, sans cookie :
			// pas de surface CSRF
			.csrf(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(requests -> requests.requestMatchers(HttpMethod.POST, "/api/auth/login")
				.permitAll()
				.requestMatchers(HttpMethod.GET, "/api/ping")
				.permitAll()
				// La documentation de l'API est publique ;
				// les routes qu'elle décrit restent protégées
				.requestMatchers(HttpMethod.GET, "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
				.permitAll()
				// Sans cela, une erreur 500 transmise à /error serait transformée en 401
				.requestMatchers("/error")
				.permitAll()
				.anyRequest()
				.authenticated())
			.oauth2ResourceServer(resourceServer -> resourceServer
				.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
				.authenticationEntryPoint(entryPoint))
			.exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(entryPoint));
		return http.build();
	}

	@Bean
	JwtDecoder jwtDecoder(JwtProperties properties) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(properties.secretKey())
			.macAlgorithm(MacAlgorithm.HS256)
			.build();
		decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(JwtService.ISSUER));
		return decoder;
	}

	@Bean
	JwtEncoder jwtEncoder(JwtProperties properties) {
		return NimbusJwtEncoder.withSecretKey(properties.secretKey()).algorithm(MacAlgorithm.HS256).build();
	}

	/** Claim "role" → autorité ROLE_xxx, pour @PreAuthorize("hasRole('ADMIN')"). */
	private static JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
		authoritiesConverter.setAuthoritiesClaimName(JwtService.ROLE_CLAIM);
		authoritiesConverter.setAuthorityPrefix("ROLE_");
		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
		return converter;
	}

}
