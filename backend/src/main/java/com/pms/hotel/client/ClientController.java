package com.pms.hotel.client;

import java.net.URI;
import java.util.UUID;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping("/api/clients")
@Tag(name = "Clients")
public class ClientController {

	private final ClientService clientService;

	public ClientController(ClientService clientService) {
		this.clientService = clientService;
	}

	/**
	 * PagedModel : format JSON de page stable, {content, page: {size, number,
	 * totalElements, totalPages}}.
	 */
	@GetMapping
	@Operation(summary = "Rechercher des clients, sans tenir compte des accents ni de la casse")
	public PagedModel<ClientResponse> search(@RequestParam(required = false) String search,
			@ParameterObject @PageableDefault(size = 20) Pageable pageable) {
		return new PagedModel<>(clientService.search(search, pageable));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Détail d'un client")
	public ClientResponse findById(@PathVariable UUID id) {
		return clientService.findById(id);
	}

	@PostMapping
	@Operation(summary = "Créer un client")
	public ResponseEntity<ClientResponse> create(@Valid @RequestBody ClientRequest request) {
		ClientResponse created = clientService.create(request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
			.path("/{id}")
			.buildAndExpand(created.id())
			.toUri();
		return ResponseEntity.created(location).body(created);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Modifier un client")
	public ClientResponse update(@PathVariable UUID id, @Valid @RequestBody ClientRequest request) {
		return clientService.update(id, request);
	}

}
