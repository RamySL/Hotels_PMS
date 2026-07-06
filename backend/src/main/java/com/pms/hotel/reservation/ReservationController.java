package com.pms.hotel.reservation;

import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/reservations")
@Tag(name = "Réservations")
public class ReservationController {

	private final ReservationService reservationService;

	public ReservationController(ReservationService reservationService) {
		this.reservationService = reservationService;
	}

	/** Réservations qui chevauchent [from, to) : ce dont a besoin une vue planning. */
	@GetMapping
	@Operation(summary = "Lister les réservations qui chevauchent la période [from, to)")
	public PagedModel<ReservationResponse> search(@RequestParam(required = false) LocalDate from,
			@RequestParam(required = false) LocalDate to, @RequestParam(required = false) ReservationStatus status,
			@ParameterObject @PageableDefault(size = 20) Pageable pageable) {
		return new PagedModel<>(reservationService.search(from, to, status, pageable));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Détail d'une réservation")
	public ReservationResponse findById(@PathVariable UUID id) {
		return reservationService.findById(id);
	}

	/** L'auteur est l'utilisateur du jeton : le claim sub contient son id. */
	@PostMapping
	@Operation(summary = "Réserver une chambre ; la facture est créée en même temps")
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
	@Operation(summary = "Modifier une réservation ; pendant le séjour, seulement la date de départ")
	public ReservationResponse update(@PathVariable UUID id, @Valid @RequestBody ReservationRequest request) {
		return reservationService.update(id, request);
	}

	@PatchMapping("/{id}/check-in")
	@Operation(summary = "Check-in : le client arrive, la chambre doit être prête")
	public ReservationResponse checkIn(@PathVariable UUID id) {
		return reservationService.checkIn(id);
	}

	@PatchMapping("/{id}/check-out")
	@Operation(summary = "Check-out : la facture doit être soldée, la chambre passe à nettoyer")
	public ReservationResponse checkOut(@PathVariable UUID id) {
		return reservationService.checkOut(id);
	}

	@PatchMapping("/{id}/cancel")
	@Operation(summary = "Annuler une réservation qui n'a pas commencé ; sa facture est annulée aussi")
	public ReservationResponse cancel(@PathVariable UUID id) {
		return reservationService.cancel(id);
	}

}
