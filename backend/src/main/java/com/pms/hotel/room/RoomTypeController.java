package com.pms.hotel.room;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/room-types")
@Tag(name = "Types de chambre")
public class RoomTypeController {

	private final RoomTypeService roomTypeService;

	public RoomTypeController(RoomTypeService roomTypeService) {
		this.roomTypeService = roomTypeService;
	}

	@GetMapping
	@Operation(summary = "Lister les types de chambre")
	public List<RoomTypeResponse> findAll() {
		return roomTypeService.findAll();
	}

	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "Créer un type de chambre (ADMIN)")
	public ResponseEntity<RoomTypeResponse> create(@Valid @RequestBody RoomTypeRequest request) {
		RoomTypeResponse created = roomTypeService.create(request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
			.path("/{id}")
			.buildAndExpand(created.id())
			.toUri();
		return ResponseEntity.created(location).body(created);
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "Modifier un type de chambre (ADMIN)")
	public RoomTypeResponse update(@PathVariable UUID id, @Valid @RequestBody RoomTypeRequest request) {
		return roomTypeService.update(id, request);
	}

}
