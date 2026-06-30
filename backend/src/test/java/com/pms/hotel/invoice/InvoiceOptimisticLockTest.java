package com.pms.hotel.invoice;

import static com.pms.hotel.TestAuthentication.RECEPTIONIST_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.pms.hotel.TestDatabase;
import com.pms.hotel.TestcontainersConfiguration;
import com.pms.hotel.client.Client;
import com.pms.hotel.client.ClientRepository;
import com.pms.hotel.reservation.Reservation;
import com.pms.hotel.reservation.ReservationRepository;
import com.pms.hotel.room.Room;
import com.pms.hotel.room.RoomRepository;
import com.pms.hotel.room.RoomType;
import com.pms.hotel.room.RoomTypeRepository;
import com.pms.hotel.user.User;
import com.pms.hotel.user.UserRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.RollbackException;

/**
 * Deux postes encaissent la même facture en même temps : sans @Version, le second écraserait
 * amount_paid et l'un des deux paiements serait perdu. Chaque poste a ici sa propre
 * transaction, d'où l'absence de transaction de test.
 */
@DataJpaTest
@Import(TestcontainersConfiguration.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class InvoiceOptimisticLockTest {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

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

	private UUID invoiceId;

	@BeforeEach
	void issueAnUnpaidInvoice() {
		RoomType doubleType = roomTypeRepository.save(new RoomType("Double", null, new BigDecimal("89.90"), 2));
		Room room101 = roomRepository.save(new Room(doubleType, "101", 1));
		Client client = clientRepository.save(new Client("Jean", "Dupont", null, null));
		LocalDate arrival = LocalDate.of(2026, 10, 10);
		Reservation reservation = reservationRepository.save(new Reservation(client, room101, arrival,
				arrival.plusDays(2), 2, userRepository.findById(RECEPTIONIST_ID).orElseThrow(), Instant.now()));
		invoiceId = invoiceRepository.save(new Invoice(reservation, Instant.now())).getId();
	}

	@AfterEach
	void deleteData() {
		TestDatabase.deleteBusinessData(jdbcTemplate);
	}

	@Test
	void secondOfTwoSimultaneousPaymentsIsRefused() {
		EntityManager frontDesk = entityManagerFactory.createEntityManager();
		EntityManager backOffice = entityManagerFactory.createEntityManager();
		try {
			frontDesk.getTransaction().begin();
			backOffice.getTransaction().begin();
			Invoice seenByFrontDesk = frontDesk.find(Invoice.class, invoiceId);
			Invoice seenByBackOffice = backOffice.find(Invoice.class, invoiceId);

			seenByBackOffice.recordPayment(new BigDecimal("50.00"), PaymentMethod.CASH,
					backOffice.getReference(User.class, RECEPTIONIST_ID), Instant.now());
			backOffice.getTransaction().commit();

			seenByFrontDesk.recordPayment(new BigDecimal("50.00"), PaymentMethod.CARD,
					frontDesk.getReference(User.class, RECEPTIONIST_ID), Instant.now());
			assertThatExceptionOfType(RollbackException.class).isThrownBy(frontDesk.getTransaction()::commit)
				.withCauseInstanceOf(OptimisticLockException.class);
		}
		finally {
			frontDesk.close();
			backOffice.close();
		}

		assertThat(jdbcTemplate.queryForObject("SELECT amount_paid FROM invoices WHERE id = ?", BigDecimal.class,
				invoiceId)).isEqualByComparingTo("50.00");
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM payments", Integer.class)).isEqualTo(1);
	}

}
