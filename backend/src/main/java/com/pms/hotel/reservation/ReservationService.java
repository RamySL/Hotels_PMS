package com.pms.hotel.reservation;

import static com.pms.hotel.reservation.ReservationSpecifications.overlapping;
import static com.pms.hotel.reservation.ReservationSpecifications.withStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pms.hotel.client.Client;
import com.pms.hotel.client.ClientRepository;
import com.pms.hotel.common.BusinessRuleException;
import com.pms.hotel.common.ResourceNotFoundException;
import com.pms.hotel.invoice.InvoiceService;
import com.pms.hotel.room.Room;
import com.pms.hotel.room.RoomRepository;
import com.pms.hotel.user.UserRepository;

@Service
public class ReservationService {

	/** Arrivées les plus proches d'abord ; l'id départage pour que les pages restent stables. */
	private static final Sort SORT = Sort.by("checkInDate", "id");

	private final ReservationRepository reservationRepository;

	private final ClientRepository clientRepository;

	private final RoomRepository roomRepository;

	private final UserRepository userRepository;

	private final AvailabilityService availabilityService;

	private final InvoiceService invoiceService;

	private final Clock clock;

	public ReservationService(ReservationRepository reservationRepository, ClientRepository clientRepository,
			RoomRepository roomRepository, UserRepository userRepository, AvailabilityService availabilityService,
			InvoiceService invoiceService, Clock clock) {
		this.reservationRepository = reservationRepository;
		this.clientRepository = clientRepository;
		this.roomRepository = roomRepository;
		this.userRepository = userRepository;
		this.availabilityService = availabilityService;
		this.invoiceService = invoiceService;
		this.clock = clock;
	}

	/** Réservations qui chevauchent [from, to), au statut donné ; chaque filtre est facultatif. */
	@Transactional(readOnly = true)
	public Page<ReservationResponse> search(LocalDate from, LocalDate to, ReservationStatus status,
			Pageable pageable) {
		PageRequest page = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), SORT);
		return reservationRepository.findAll(Specification.allOf(overlapping(from, to), withStatus(status)), page)
			.map(ReservationResponse::from);
	}

	@Transactional(readOnly = true)
	public ReservationResponse findById(UUID id) {
		return ReservationResponse.from(findReservation(id));
	}

	@Transactional
	public ReservationResponse create(ReservationRequest request, UUID userId) {
		ensureNotInThePast(request.checkInDate(), "La date d'arrivée");
		Client client = clientRepository.findById(request.clientId())
			.orElseThrow(() -> new ResourceNotFoundException("Client", request.clientId()));
		Room room = findRoom(request.roomId());
		Reservation reservation = new Reservation(client, room, request.checkInDate(), request.checkOutDate(),
				request.numberOfGuests(), userRepository.getReferenceById(userId), Instant.now(clock));
		availabilityService.ensureBookable(room, request.checkInDate(), request.checkOutDate(), null);
		reservationRepository.save(reservation);
		flushOrRefuseOverlap(room);
		invoiceService.createFor(reservation);
		return ReservationResponse.from(reservation);
	}

	/**
	 * Les règles liées au statut passent avant le contrôle de disponibilité, et la réservation
	 * n'est modifiée qu'après : ce contrôle ne doit pas voir une modification encore en attente.
	 */
	@Transactional
	public ReservationResponse update(UUID id, ReservationRequest request) {
		Reservation reservation = findReservation(id);
		Room room = findRoom(request.roomId());
		reservation.checkCanBeRescheduled(room, request.checkInDate(), request.numberOfGuests());
		if (!request.checkInDate().equals(reservation.getCheckInDate())) {
			ensureNotInThePast(request.checkInDate(), "La date d'arrivée");
		}
		if (reservation.getStatus() == ReservationStatus.CHECKED_IN) {
			ensureNotInThePast(request.checkOutDate(), "La date de départ");
		}
		availabilityService.ensureBookable(room, request.checkInDate(), request.checkOutDate(), id);
		reservation.reschedule(room, request.checkInDate(), request.checkOutDate(), request.numberOfGuests());
		flushOrRefuseOverlap(room);
		invoiceService.updateAmount(reservation);
		return ReservationResponse.from(reservation);
	}

	@Transactional
	public ReservationResponse checkIn(UUID id) {
		Reservation reservation = findReservation(id);
		reservation.checkIn(LocalDate.now(clock));
		return ReservationResponse.from(reservation);
	}

	@Transactional
	public ReservationResponse checkOut(UUID id) {
		Reservation reservation = findReservation(id);
		invoiceService.ensureSettled(reservation);
		reservation.checkOut();
		return ReservationResponse.from(reservation);
	}

	@Transactional
	public ReservationResponse cancel(UUID id) {
		Reservation reservation = findReservation(id);
		reservation.cancel();
		invoiceService.cancelFor(reservation);
		return ReservationResponse.from(reservation);
	}

	private void ensureNotInThePast(LocalDate date, String label) {
		if (date.isBefore(LocalDate.now(clock))) {
			throw new BusinessRuleException(label + " ne peut pas être dans le passé");
		}
	}

	/**
	 * Filet de sécurité (ADR-006) : si deux réservations simultanées ont passé le contrôle,
	 * la contrainte ex_reservations_no_overlap refuse la seconde ; on renvoie alors le même 409.
	 */
	private void flushOrRefuseOverlap(Room room) {
		try {
			reservationRepository.flush();
		}
		catch (DataIntegrityViolationException ex) {
			if (ex.getMessage() != null && ex.getMessage().contains("ex_reservations_no_overlap")) {
				throw new BusinessRuleException(
						"La chambre " + room.getRoomNumber() + " est déjà réservée sur cette période");
			}
			throw ex;
		}
	}

	private Reservation findReservation(UUID id) {
		return reservationRepository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("Réservation", id));
	}

	private Room findRoom(UUID id) {
		return roomRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Chambre", id));
	}

}
