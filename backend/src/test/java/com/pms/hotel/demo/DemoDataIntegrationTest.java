package com.pms.hotel.demo;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.pms.hotel.TestcontainersConfiguration;
import com.pms.hotel.dashboard.DashboardResponse;
import com.pms.hotel.dashboard.DashboardResponse.Stay;
import com.pms.hotel.dashboard.DashboardService;
import com.pms.hotel.room.RoomResponse;

/**
 * Données de démo créées au démarrage avec le profil demo, sur un vrai PostgreSQL. Ce
 * contexte a sa propre base : les autres tests d'intégration n'en voient rien.
 */
@SpringBootTest
@ActiveProfiles("demo")
@Import(TestcontainersConfiguration.class)
class DemoDataIntegrationTest {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private DashboardService dashboardService;

	@Autowired
	private DemoData demoData;

	@Autowired
	private Clock clock;

	@Test
	void fillsTheHotelWithEveryKindOfStayAndInvoice() {
		assertThat(count("room_types")).isEqualTo(4);
		assertThat(count("rooms")).isEqualTo(10);
		assertThat(count("clients")).isEqualTo(12);
		assertThat(count("reservations")).isEqualTo(15);
		assertThat(distinct("SELECT DISTINCT status FROM reservations")).containsExactlyInAnyOrder("CONFIRMED",
				"CHECKED_IN", "CHECKED_OUT", "CANCELLED");
		assertThat(distinct("SELECT DISTINCT status FROM invoices")).containsExactlyInAnyOrder("UNPAID",
				"PARTIALLY_PAID", "PAID", "CANCELLED");
		assertThat(distinct("SELECT DISTINCT status FROM rooms")).containsExactlyInAnyOrder("READY", "OCCUPIED",
				"CLEANING", "OUT_OF_SERVICE");
	}

	/**
	 * Le tableau de bord de la démo a de quoi faire, quel que soit le jour où elle
	 * démarre.
	 */
	@Test
	void givesTheReceptionAFullDay() {
		LocalDate today = LocalDate.now(clock);

		DashboardResponse day = dashboardService.today();

		assertThat(day.arrivals()).extracting(Stay::clientName).containsExactly("Sophie Bernard", "Hélène Dupont");
		assertThat(day.arrivals()).filteredOn(stay -> stay.checkInDate().isBefore(today))
			.extracting(Stay::clientName)
			.containsExactly("Sophie Bernard");
		assertThat(day.departures()).extracting(Stay::clientName).containsExactly("Léa Petit", "Marc Leroy");
		assertThat(day.departures()).filteredOn(stay -> stay.checkOutDate().isBefore(today))
			.extracting(Stay::clientName)
			.containsExactly("Marc Leroy");
		assertThat(day.occupancy().occupiedRooms()).isEqualTo(4);
		assertThat(day.occupancy().roomsInService()).isEqualTo(9);
		assertThat(day.roomsToClean()).extracting(RoomResponse::roomNumber).containsExactly("105", "201");
	}

	@Test
	void createsNothingMoreOnTheNextStart() {
		demoData.run(null);

		assertThat(count("rooms")).isEqualTo(10);
		assertThat(count("clients")).isEqualTo(12);
		assertThat(count("reservations")).isEqualTo(15);
	}

	private long count(String table) {
		return jdbcTemplate.queryForObject("SELECT count(*) FROM " + table, Long.class);
	}

	private List<String> distinct(String sql) {
		return jdbcTemplate.queryForList(sql, String.class);
	}

}
