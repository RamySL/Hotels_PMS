package com.pms.hotel.reservation;

import static com.pms.hotel.reservation.ReservationSpecifications.active;
import static com.pms.hotel.reservation.ReservationSpecifications.excluding;
import static com.pms.hotel.reservation.ReservationSpecifications.forRoom;
import static com.pms.hotel.reservation.ReservationSpecifications.overlapping;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pms.hotel.common.BusinessRuleException;
import com.pms.hotel.room.Room;
import com.pms.hotel.room.RoomRepository;
import com.pms.hotel.room.RoomStatus;

/**
 * Disponibilité des chambres. La contrainte ex_reservations_no_overlap garantit en base
 * qu'il n'y a jamais de double réservation ; ce service vérifie avant, pour un message
 * clair.
 */
@Service
public class AvailabilityService {

	private final ReservationRepository reservationRepository;

	private final RoomRepository roomRepository;

	public AvailabilityService(ReservationRepository reservationRepository, RoomRepository roomRepository) {
		this.reservationRepository = reservationRepository;
		this.roomRepository = roomRepository;
	}

	/**
	 * Refuse (409) une chambre hors service ou déjà réservée sur la période.
	 * reservationId : la réservation modifiée, qui ne doit pas entrer en conflit avec
	 * elle-même (null à la création).
	 */
	@Transactional(readOnly = true)
	public void ensureBookable(Room room, LocalDate checkIn, LocalDate checkOut, UUID reservationId) {
		if (room.getStatus() == RoomStatus.OUT_OF_SERVICE) {
			throw new BusinessRuleException("La chambre " + room.getRoomNumber() + " est hors service");
		}
		if (reservationRepository.exists(Specification.allOf(forRoom(room.getId()), active(),
				overlapping(checkIn, checkOut), excluding(reservationId)))) {
			throw new BusinessRuleException(
					"La chambre " + room.getRoomNumber() + " est déjà réservée sur cette période");
		}
	}

	/** Chambres libres sur la période, avec le prix du séjour de chacune. */
	@Transactional(readOnly = true)
	public List<AvailableRoomResponse> findAvailableRoomsWithPrice(LocalDate checkIn, LocalDate checkOut) {
		return findAvailableRooms(checkIn, checkOut).stream()
			.map(room -> AvailableRoomResponse.of(room, checkIn, checkOut))
			.toList();
	}

	/**
	 * Chambres libres sur la période, sans les chambres hors service, par étage puis
	 * numéro.
	 */
	@Transactional(readOnly = true)
	public List<Room> findAvailableRooms(LocalDate checkIn, LocalDate checkOut) {
		Set<UUID> bookedRoomIds = reservationRepository
			.findAll(Specification.allOf(active(), overlapping(checkIn, checkOut)))
			.stream()
			.map(reservation -> reservation.getRoom().getId())
			.collect(Collectors.toSet());
		return roomRepository.findAllByOrderByFloorAscRoomNumberAsc()
			.stream()
			.filter(room -> room.getStatus() != RoomStatus.OUT_OF_SERVICE && !bookedRoomIds.contains(room.getId()))
			.toList();
	}

}
