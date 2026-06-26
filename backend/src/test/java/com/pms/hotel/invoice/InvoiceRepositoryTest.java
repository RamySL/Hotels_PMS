package com.pms.hotel.invoice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.pms.hotel.TestcontainersConfiguration;
import com.pms.hotel.client.Client;
import com.pms.hotel.client.ClientRepository;
import com.pms.hotel.reservation.Reservation;
import com.pms.hotel.reservation.ReservationRepository;
import com.pms.hotel.room.Room;
import com.pms.hotel.room.RoomRepository;
import com.pms.hotel.room.RoomType;
import com.pms.hotel.room.RoomTypeRepository;
import com.pms.hotel.user.UserRepository;

@DataJpaTest
@Import(TestcontainersConfiguration.class)
class InvoiceRepositoryTest {

	@Autowired
	private InvoiceRepository invoiceRepository;

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

	private Reservation twoNights;

	@BeforeEach
	void bookTwoNights() {
		RoomType doubleType = roomTypeRepository.save(new RoomType("Double", null, new BigDecimal("89.90"), 2));
		Room room101 = roomRepository.save(new Room(doubleType, "101", 1));
		Client client = clientRepository.save(new Client("Jean", "Dupont", null, null));
		LocalDate arrival = LocalDate.of(2026, 10, 10);
		twoNights = reservationRepository.save(new Reservation(client, room101, arrival, arrival.plusDays(2), 2,
				userRepository.findByEmail("reception@pms.local").orElseThrow(), Instant.now()));
	}

	@Test
	void newInvoiceIsUnpaidForThePriceOfTheStay() {
		Invoice invoice = invoiceRepository.saveAndFlush(new Invoice(twoNights, Instant.now()));

		assertThat(jdbcTemplate.queryForMap(
				"SELECT amount_total, amount_paid, status, version FROM invoices WHERE id = ?", invoice.getId()))
			.containsEntry("amount_total", new BigDecimal("179.80"))
			.containsEntry("amount_paid", new BigDecimal("0.00"))
			.containsEntry("status", "UNPAID")
			.containsEntry("version", 0L);
	}

	@Test
	void reservationHasOnlyOneInvoice() {
		invoiceRepository.saveAndFlush(new Invoice(twoNights, Instant.now()));

		assertThatExceptionOfType(DataIntegrityViolationException.class)
			.isThrownBy(() -> invoiceRepository.saveAndFlush(new Invoice(twoNights, Instant.now())))
			.withMessageContaining("uk_invoices_reservation");
	}

}
