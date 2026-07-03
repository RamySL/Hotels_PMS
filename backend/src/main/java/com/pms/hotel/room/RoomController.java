package com.pms.hotel.room;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

	private final RoomService roomService;

	public RoomController(RoomService roomService) {
		this.roomService = roomService;
	}

	@GetMapping
	public List<RoomResponse> findAll(@RequestParam(required = false) RoomStatus status) {
		return roomService.findAll(status);
	}

	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<RoomResponse> create(@Valid @RequestBody RoomRequest request) {
		RoomResponse created = roomService.create(request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
			.path("/{id}")
			.buildAndExpand(created.id())
			.toUri();
		return ResponseEntity.created(location).body(created);
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public RoomResponse update(@PathVariable UUID id, @Valid @RequestBody RoomRequest request) {
		return roomService.update(id, request);
	}

	/**
	 * Ouvert aux deux rôles : la réception remet une chambre en service après le ménage.
	 */
	@PatchMapping("/{id}/status")
	public RoomResponse changeStatus(@PathVariable UUID id, @Valid @RequestBody RoomStatusRequest request) {
		return roomService.changeStatus(id, request.status());
	}

}
