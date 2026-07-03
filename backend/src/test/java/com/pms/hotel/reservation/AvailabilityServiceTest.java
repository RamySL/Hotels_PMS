package com.pms.hotel.reservation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import com.pms.hotel.TestcontainersConfiguration;
import com.pms.hotel.client.Client;
import com.pms.hotel.client.ClientRepository;
import com.pms.hotel.common.BusinessRuleException;
import com.pms.hotel.room.Room;
import com.pms.hotel.room.RoomRepository;
import com.pms.hotel.room.RoomStatus;
import com.pms.hotel.room.RoomType;
import com.pms.hotel.room.RoomTypeRepository;
import com.pms.hotel.user.UserRepository;

/**
 * Cas limites de la disponibilité, sur une vraie base : la logique est dans les requêtes.
 * La chambre 101 est réservée du 10 au 14.
 */
@DataJpaTest
@Import({ TestcontainersConfiguration.class, AvailabilityService.class })
class AvailabilityServiceTest {

	@Autowired
	private AvailabilityService availabilityService;

	@Autowired
	private ReservationRepository reservationRepository;

	@Autowired
	private RoomTypeRepository roomTypeRepository;

	@Autowired
	private RoomRepository roomRepository;

	@Autowired
	private ClientRepository clientRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private RoomType doubleType;

	private Room room101;

	private Room room102;

	private Reservation existing;

	@BeforeEach
	void bookRoom101From10To14() {
		doubleType = roomTypeRepository.save(new RoomType("Double", null, new BigDecimal("89.90"), 2));
		room101 = roomRepository.save(new Room(doubleType, "101", 1));
		room102 = roomRepository.save(new Room(doubleType, "102", 1));
		Client client = clientRepository.save(new Client("Jean", "Dupont", null, null));
		existing = reservationRepository.saveAndFlush(new Reservation(client, room101, day(10), day(14), 2,
				userRepository.findByEmail("reception@pms.local").orElseThrow(), Instant.now()));
	}

	// La 3e colonne ne sert qu'à nommer le cas dans les rapports de test
	@ParameterizedTest(name = "{2}")
	@CsvSource({ "10, 14, mêmes dates", "12, 16, commence pendant le séjour", "8, 11, finit pendant le séjour",
			"11, 13, incluse dans le séjour", "8, 16, englobe le séjour" })
	void periodSharingANightIsRefused(int from, int to, String scenario) {
		assertThatExceptionOfType(BusinessRuleException.class)
			.isThrownBy(() -> availabilityService.ensureBookable(room101, day(from), day(to), null))
			.withMessage("La chambre 101 est déjà réservée sur cette période");
	}

	// Bornes : un départ et une arrivée le même jour ne partagent aucune nuit
	@ParameterizedTest(name = "{2}")
	@CsvSource({ "6, 10, part le jour où le séjour commence", "14, 16, arrive le jour où le séjour finit" })
	void periodAdjacentToTheStayIsFree(int from, int to, String scenario) {
		assertThatCode(() -> availabilityService.ensureBookable(room101, day(from), day(to), null))
			.doesNotThrowAnyException();
	}

	@Test
	void roomIsAvailableFromThePreviousGuestDepartureDay() {
		assertThat(availabilityService.findAvailableRooms(day(14), day(16))).extracting(Room::getRoomNumber)
			.containsExactly("101", "102");
	}

	@ParameterizedTest
	@CsvSource({ "CANCELLED", "CHECKED_OUT" })
	void cancelledOrFinishedReservationNoLongerHoldsTheRoom(String status) {
		// Annulation et check-out arrivent avec le cycle de vie : on les simule en base
		jdbcTemplate.update("UPDATE reservations SET status = ? WHERE id = ?", status, existing.getId());

		assertThatCode(() -> availabilityService.ensureBookable(room101, day(10), day(14), null))
			.doesNotThrowAnyException();
	}

	@Test
	void stayInProgressStillHoldsTheRoom() {
		jdbcTemplate.update("UPDATE reservations SET status = 'CHECKED_IN' WHERE id = ?", existing.getId());

		assertThatExceptionOfType(BusinessRuleException.class)
			.isThrownBy(() -> availabilityService.ensureBookable(room101, day(11), day(12), null));
	}

	@Test
	void reservationsOfAnotherRoomAreIgnored() {
		assertThatCode(() -> availabilityService.ensureBookable(room102, day(10), day(14), null))
			.doesNotThrowAnyException();
	}

	@Test
	void reservationBeingModifiedDoesNotConflictWithItself() {
		assertThatCode(() -> availabilityService.ensureBookable(room101, day(11), day(13), existing.getId()))
			.doesNotThrowAnyException();
	}

	@Test
	void roomOutOfServiceCannotBeBooked() {
		room102.changeStatus(RoomStatus.OUT_OF_SERVICE);

		assertThatExceptionOfType(BusinessRuleException.class)
			.isThrownBy(() -> availabilityService.ensureBookable(room102, day(20), day(22), null))
			.withMessage("La chambre 102 est hors service");
	}

	@Test
	void availableRoomsExcludeBookedAndOutOfServiceRooms() {
		Room room103 = roomRepository.save(new Room(doubleType, "103", 1));
		room103.changeStatus(RoomStatus.OUT_OF_SERVICE);

		assertThat(availabilityService.findAvailableRooms(day(10), day(14))).extracting(Room::getRoomNumber)
			.containsExactly("102");
	}

	private static LocalDate day(int dayOfMonth) {
		return LocalDate.of(2026, 10, dayOfMonth);
	}

}
