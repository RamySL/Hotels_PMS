package com.pms.hotel.client;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<Client, UUID> {

	/** « Containing » : Spring Data échappe les caractères % et _ du terme recherché. */
	Page<Client> findBySearchTextContaining(String searchKey, Pageable pageable);

}
