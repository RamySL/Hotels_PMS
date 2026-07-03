package com.pms.hotel.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.pms.hotel.dashboard.DashboardResponse.Occupancy;

class OccupancyTest {

	/** Des taux qui tombent juste (1 sur 8) comme des taux sans fin (1 sur 3, 17 sur 23). */
	@ParameterizedTest(name = "{0} chambres sur {1} : {2} %")
	@CsvSource({ "1, 3, 33.3", "2, 3, 66.7", "1, 7, 14.3", "17, 23, 73.9", "1, 8, 12.5", "1, 16, 6.3", "0, 5, 0.0",
			"5, 5, 100.0" })
	void rateIsRoundedToOneDecimalWhateverTheNumberOfRooms(long occupiedRooms, long roomsInService,
			String expectedRate) {
		assertThat(Occupancy.of(occupiedRooms, roomsInService).rate()).isEqualByComparingTo(expectedRate);
	}

}
