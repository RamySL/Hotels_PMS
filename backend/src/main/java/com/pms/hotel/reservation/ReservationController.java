package com.pms.hotel.reservation;

import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

	private final ReservationService reservationService;

	public ReservationController(ReservationService reservationService) {
		this.reservationService = reservationService;
	}

	/** Réservations qui chevauchent [from, to) : ce dont a besoin une vue planning. */
	@GetMapping
	public PagedModel<ReservationResponse> search(@RequestParam(required = false) LocalDate from,
			@RequestParam(required = false) LocalDate to, @RequestParam(required = false) ReservationStatus status,
			@PageableDefault(size = 20) Pageable pageable) {
		return new PagedModel<>(reservationService.search(from, to, status, pageable));
	}

	@GetMapping("/{id}")
	public ReservationResponse findById(@PathVariable UUID id) {
		return reservationService.findById(id);
	}

	/** L'auteur est l'utilisateur du jeton : le claim sub contient son id. */
	@PostMapping
	public ResponseEntity<ReservationResponse> create(@Valid @RequestBody ReservationRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		ReservationResponse created = reservationService.create(request, UUID.fromString(jwt.getSubject()));
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
			.path("/{id}")
			.buildAndExpand(created.id())
			.toUri();
		return ResponseEntity.created(location).body(created);
	}

	@PutMapping("/{id}")
	public ReservationResponse update(@PathVariable UUID id, @Valid @RequestBody ReservationRequest request) {
		return reservationService.update(id, request);
	}

}
