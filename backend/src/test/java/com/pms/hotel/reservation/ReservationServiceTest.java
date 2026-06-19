package com.pms.hotel.reservation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.pms.hotel.client.Client;
import com.pms.hotel.client.ClientRepository;
import com.pms.hotel.common.BusinessRuleException;
import com.pms.hotel.common.ResourceNotFoundException;
import com.pms.hotel.room.Room;
import com.pms.hotel.room.RoomRepository;
import com.pms.hotel.room.RoomType;
import com.pms.hotel.user.Role;
import com.pms.hotel.user.User;
import com.pms.hotel.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

	private static final ZoneId PARIS = ZoneId.of("Europe/Paris");

	private static final LocalDate TODAY = LocalDate.of(2026, 10, 10);

	private static final UUID CLIENT_ID = UUID.randomUUID();

	private static final UUID ROOM_ID = UUID.randomUUID();

	private static final UUID USER_ID = UUID.randomUUID();

	private static final UUID RESERVATION_ID = UUID.randomUUID();

	@Mock
	private ReservationRepository reservationRepository;

	@Mock
	private ClientRepository clientRepository;

	@Mock
	private RoomRepository roomRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private AvailabilityService availabilityService;

	private ReservationService reservationService;

	private final Room room101 = new Room(new RoomType("Double", null, new BigDecimal("89.90"), 2), "101", 1);

	private final Client client = new Client("Jean", "Dupont", null, null);

	private final User receptionist = new User("reception@pms.local", "hash", "Lucas", "Bernard",
			Role.RECEPTIONIST);

	@BeforeEach
	void setUp() {
		Clock clock = Clock.fixed(TODAY.atStartOfDay(PARIS).toInstant(), PARIS);
		reservationService = new ReservationService(reservationRepository, clientRepository, roomRepository,
				userRepository, availabilityService, clock);
	}

	@Test
	void bookingIsConfirmedPricedAndAttributedToTheUser() {
		stubClientRoomAndUser();

		ReservationResponse created = reservationService.create(request(TODAY, TODAY.plusDays(3), 2), USER_ID);

		assertThat(created.status()).isEqualTo(ReservationStatus.CONFIRMED);
		assertThat(created.totalPrice()).isEqualByComparingTo("269.70");
		assertThat(created.createdBy().lastName()).isEqualTo("Bernard");
		verify(availabilityService).ensureBookable(room101, TODAY, TODAY.plusDays(3), null);
		verify(reservationRepository).save(any(Reservation.class));
	}

	@Test
	void arrivalInThePastIsRefused() {
		assertThatExceptionOfType(BusinessRuleException.class)
			.isThrownBy(() -> reservationService.create(request(TODAY.minusDays(1), TODAY.plusDays(1), 2), USER_ID))
			.withMessage("La date d'arrivée ne peut pas être dans le passé");
		verifyNoInteractions(reservationRepository);
	}

	@Test
	void unknownClientIsNotFound() {
		when(clientRepository.findById(CLIENT_ID)).thenReturn(Optional.empty());

		assertThatExceptionOfType(ResourceNotFoundException.class)
			.isThrownBy(() -> reservationService.create(request(TODAY, TODAY.plusDays(1), 2), USER_ID))
			.withMessage("Client introuvable : " + CLIENT_ID);
	}

	@Test
	void unavailableRoomIsNotBooked() {
		stubClientRoomAndUser();
		doThrow(new BusinessRuleException("La chambre 101 est déjà réservée sur cette période"))
			.when(availabilityService)
			.ensureBookable(room101, TODAY, TODAY.plusDays(2), null);

		assertThatExceptionOfType(BusinessRuleException.class)
			.isThrownBy(() -> reservationService.create(request(TODAY, TODAY.plusDays(2), 2), USER_ID));
		verify(reservationRepository, never()).save(any());
	}

	@Test
	void overlapRefusedByTheDatabaseGivesTheSameConflict() {
		stubClientRoomAndUser();
		doThrow(new DataIntegrityViolationException("violates exclusion constraint \"ex_reservations_no_overlap\""))
			.when(reservationRepository)
			.flush();

		assertThatExceptionOfType(BusinessRuleException.class)
			.isThrownBy(() -> reservationService.create(request(TODAY, TODAY.plusDays(2), 2), USER_ID))
			.withMessage("La chambre 101 est déjà réservée sur cette période");
	}

	@Test
	void otherIntegrityViolationsAreNotHidden() {
		stubClientRoomAndUser();
		doThrow(new DataIntegrityViolationException("autre contrainte")).when(reservationRepository).flush();

		assertThatExceptionOfType(DataIntegrityViolationException.class)
			.isThrownBy(() -> reservationService.create(request(TODAY, TODAY.plusDays(2), 2), USER_ID));
	}

	@Test
	void modificationIsCheckedWithoutConflictingWithItselfAndRepriced() {
		Reservation reservation = existing(TODAY.plusDays(1), TODAY.plusDays(3));
		when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));
		when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room101));

		ReservationResponse updated = reservationService.update(RESERVATION_ID,
				request(TODAY.plusDays(2), TODAY.plusDays(5), 2));

		verify(availabilityService).ensureBookable(room101, TODAY.plusDays(2), TODAY.plusDays(5), RESERVATION_ID);
		assertThat(updated.nights()).isEqualTo(3);
		assertThat(updated.totalPrice()).isEqualByComparingTo("269.70");
	}

	@Test
	void unchangedPastArrivalDoesNotBlockAModification() {
		Reservation lateArrival = existing(TODAY.minusDays(1), TODAY.plusDays(2));
		when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(lateArrival));
		when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room101));

		reservationService.update(RESERVATION_ID, request(TODAY.minusDays(1), TODAY.plusDays(3), 2));

		assertThat(lateArrival.getCheckOutDate()).isEqualTo(TODAY.plusDays(3));
	}

	@Test
	void modificationOfUnknownReservationIsNotFound() {
		when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.empty());

		assertThatExceptionOfType(ResourceNotFoundException.class)
			.isThrownBy(() -> reservationService.update(RESERVATION_ID, request(TODAY, TODAY.plusDays(1), 2)));
	}

	private void stubClientRoomAndUser() {
		when(clientRepository.findById(CLIENT_ID)).thenReturn(Optional.of(client));
		when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room101));
		when(userRepository.getReferenceById(USER_ID)).thenReturn(receptionist);
	}

	private Reservation existing(LocalDate checkIn, LocalDate checkOut) {
		return new Reservation(client, room101, checkIn, checkOut, 2, receptionist, Instant.EPOCH);
	}

	private static ReservationRequest request(LocalDate checkIn, LocalDate checkOut, int guests) {
		return new ReservationRequest(CLIENT_ID, ROOM_ID, checkIn, checkOut, guests);
	}

}
