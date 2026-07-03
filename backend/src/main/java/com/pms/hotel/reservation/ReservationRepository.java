package com.pms.hotel.reservation;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface ReservationRepository extends JpaRepository<Reservation, UUID>, JpaSpecificationExecutor<Reservation> {

	// La liste affiche le client, la chambre, son type et l'auteur : chargés dans la même
	// requête (jointures) plutôt qu'une requête par réservation (N+1)
	@Override
	@EntityGraph(attributePaths = { "client", "room", "room.roomType", "createdBy" })
	Page<Reservation> findAll(Specification<Reservation> spec, Pageable pageable);

	/**
	 * Arrivées à faire ce jour-là : réservations confirmées dont le check-in est encore possible,
	 * y compris un client attendu un jour précédent tant que son séjour n'est pas fini. Client et
	 * chambre sont chargés dans la même requête.
	 */
	@Query("""
			select r from Reservation r join fetch r.client join fetch r.room ro
			where r.status = com.pms.hotel.reservation.ReservationStatus.CONFIRMED
				and r.checkInDate <= :day and r.checkOutDate > :day
			order by ro.floor, ro.roomNumber""")
	List<Reservation> findExpectedArrivals(LocalDate day);

	/**
	 * Départs à faire ce jour-là : clients arrivés dont le départ est prévu ce jour-là ou avant
	 * (check-out oublié). Client et chambre sont chargés dans la même requête.
	 */
	@Query("""
			select r from Reservation r join fetch r.client join fetch r.room ro
			where r.status = com.pms.hotel.reservation.ReservationStatus.CHECKED_IN
				and r.checkOutDate <= :day
			order by ro.floor, ro.roomNumber""")
	List<Reservation> findExpectedDepartures(LocalDate day);

}
