package com.pms.hotel.reservation;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ReservationRepository extends JpaRepository<Reservation, UUID>, JpaSpecificationExecutor<Reservation> {

	// La liste affiche le client, la chambre, son type et l'auteur : chargés dans la même
	// requête (jointures) plutôt qu'une requête par réservation (N+1)
	@Override
	@EntityGraph(attributePaths = { "client", "room", "room.roomType", "createdBy" })
	Page<Reservation> findAll(Specification<Reservation> spec, Pageable pageable);

}
