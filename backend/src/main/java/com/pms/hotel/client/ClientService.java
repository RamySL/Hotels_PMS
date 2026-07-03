package com.pms.hotel.client;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pms.hotel.common.ResourceNotFoundException;

@Service
public class ClientService {

	/**
	 * Ordre alphabétique ; l'id départage les homonymes pour que les pages restent
	 * stables.
	 */
	private static final Sort SORT = Sort.by("lastName", "firstName", "id");

	private final ClientRepository clientRepository;

	public ClientService(ClientRepository clientRepository) {
		this.clientRepository = clientRepository;
	}

	/**
	 * Le tri demandé par le client est ignoré : seul l'ordre alphabétique est proposé.
	 */
	@Transactional(readOnly = true)
	public Page<ClientResponse> search(String term, Pageable pageable) {
		PageRequest page = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), SORT);
		Page<Client> clients = term == null || term.isBlank() ? clientRepository.findAll(page)
				: clientRepository.findBySearchTextContaining(ClientSearch.searchKey(term), page);
		return clients.map(ClientResponse::from);
	}

	@Transactional(readOnly = true)
	public ClientResponse findById(UUID id) {
		return ClientResponse.from(findClient(id));
	}

	@Transactional
	public ClientResponse create(ClientRequest request) {
		Client client = new Client(request.firstName(), request.lastName(), request.email(), request.phone());
		return ClientResponse.from(clientRepository.save(client));
	}

	/** Remplace toute la fiche : un email ou un téléphone omis est effacé. */
	@Transactional
	public ClientResponse update(UUID id, ClientRequest request) {
		Client client = findClient(id);
		client.update(request.firstName(), request.lastName(), request.email(), request.phone());
		return ClientResponse.from(client);
	}

	private Client findClient(UUID id) {
		return clientRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Client", id));
	}

}
