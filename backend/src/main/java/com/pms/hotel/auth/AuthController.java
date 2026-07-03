package com.pms.hotel.auth;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pms.hotel.user.User;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthenticationManager authenticationManager;

	private final JwtService jwtService;

	public AuthController(AuthenticationManager authenticationManager, JwtService jwtService) {
		this.authenticationManager = authenticationManager;
		this.jwtService = jwtService;
	}

	/**
	 * Un échec lève une AuthenticationException, traduite en 401 par
	 * GlobalExceptionHandler.
	 */
	@PostMapping("/login")
	public LoginResponse login(@Valid @RequestBody LoginRequest request) {
		Authentication authentication = authenticationManager
			.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(request.email(), request.password()));
		User user = ((AuthenticatedUser) authentication.getPrincipal()).user();
		String accessToken = jwtService.generateToken(user.getId(), user.getRole());
		return LoginResponse.bearer(accessToken, jwtService.expiration(), user);
	}

}
