package com.pms.hotel.room;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.pms.hotel.TestcontainersConfiguration;

@DataJpaTest
@Import(TestcontainersConfiguration.class)
class RoomRepositoryTest {

	@Autowired
	private RoomTypeRepository roomTypeRepository;

	@Autowired
	private RoomRepository roomRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void newRoomIsStoredAsReady() {
		Room room = roomRepository.saveAndFlush(new Room(roomType("Double"), " 101 ", 1));

		assertThat(room.getRoomNumber()).isEqualTo("101");
		assertThat(jdbcTemplate.queryForObject("SELECT status FROM rooms WHERE id = ?", String.class, room.getId()))
			.isEqualTo("READY");
	}

	@Test
	void roomTypeNameIsUniqueWhateverTheCase() {
		roomType("Suite");

		assertThatExceptionOfType(DataIntegrityViolationException.class).isThrownBy(() -> roomType(" SUITE "))
			.withMessageContaining("uk_room_types_name");
	}

	@Test
	void roomNumberIsUnique() {
		RoomType twin = roomType("Twin");
		roomRepository.saveAndFlush(new Room(twin, "102", 1));

		assertThatExceptionOfType(DataIntegrityViolationException.class)
			.isThrownBy(() -> roomRepository.saveAndFlush(new Room(twin, "102", 1)))
			.withMessageContaining("uk_rooms_room_number");
	}

	private RoomType roomType(String name) {
		return roomTypeRepository.saveAndFlush(new RoomType(name, null, new BigDecimal("89.90"), 2));
	}

}
