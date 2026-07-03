package com.pms.hotel.room;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.pms.hotel.common.BusinessRuleException;

class RoomTest {

	private final Room room = new Room(new RoomType("Double", null, new BigDecimal("89.90"), 2), " 101 ", 1);

	@Test
	void newRoomIsReadyWithTrimmedNumber() {
		assertThat(room.getStatus()).isEqualTo(RoomStatus.READY);
		assertThat(room.getRoomNumber()).isEqualTo("101");
	}

	@ParameterizedTest
	@EnumSource(value = RoomStatus.class, names = { "READY", "CLEANING", "OUT_OF_SERVICE" })
	void staffCanSetManualStatuses(RoomStatus status) {
		room.changeStatus(status);

		assertThat(room.getStatus()).isEqualTo(status);
	}

	@Test
	void occupiedCannotBeSetManually() {
		assertThatExceptionOfType(BusinessRuleException.class).isThrownBy(() -> room.changeStatus(RoomStatus.OCCUPIED))
			.withMessage("Le statut OCCUPIED est posé par le check-in, pas manuellement");
		assertThat(room.getStatus()).isEqualTo(RoomStatus.READY);
	}

	@Test
	void checkInOccupiesAReadyRoomAndCheckOutLeavesItToClean() {
		room.checkIn();
		assertThat(room.getStatus()).isEqualTo(RoomStatus.OCCUPIED);

		room.checkOut();
		assertThat(room.getStatus()).isEqualTo(RoomStatus.CLEANING);
	}

	@Test
	void checkInRequiresAReadyRoom() {
		room.changeStatus(RoomStatus.CLEANING);

		assertThatExceptionOfType(BusinessRuleException.class).isThrownBy(room::checkIn)
			.withMessage("La chambre 101 n'est pas prête (CLEANING)");
	}

}
