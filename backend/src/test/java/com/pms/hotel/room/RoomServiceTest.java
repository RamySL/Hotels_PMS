package com.pms.hotel.room;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.pms.hotel.common.BusinessRuleException;
import com.pms.hotel.common.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

	private static final UUID TYPE_ID = UUID.randomUUID();

	@Mock
	private RoomRepository roomRepository;

	@Mock
	private RoomTypeRepository roomTypeRepository;

	@InjectMocks
	private RoomService roomService;

	private final RoomType doubleRoom = new RoomType("Double", null, new BigDecimal("89.90"), 2);

	@Test
	void listsRoomsWithTheirType() {
		when(roomRepository.findAllByOrderByFloorAscRoomNumberAsc())
			.thenReturn(List.of(new Room(doubleRoom, "101", 1)));

		assertThat(roomService.findAll(null)).singleElement().satisfies(room -> {
			assertThat(room.roomNumber()).isEqualTo("101");
			assertThat(room.roomType().name()).isEqualTo("Double");
		});
	}

	@Test
	void filtersRoomsByStatus() {
		when(roomRepository.findByStatusOrderByFloorAscRoomNumberAsc(RoomStatus.CLEANING)).thenReturn(List.of());

		assertThat(roomService.findAll(RoomStatus.CLEANING)).isEmpty();
	}

	@Test
	void createsReadyRoomOfExistingType() {
		when(roomTypeRepository.findById(TYPE_ID)).thenReturn(Optional.of(doubleRoom));
		when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));

		RoomResponse created = roomService.create(new RoomRequest(TYPE_ID, " 101 ", 1));

		assertThat(created.roomNumber()).isEqualTo("101");
		assertThat(created.status()).isEqualTo(RoomStatus.READY);
		assertThat(created.roomType().name()).isEqualTo("Double");
	}

	@Test
	void refusesExistingRoomNumber() {
		when(roomTypeRepository.findById(TYPE_ID)).thenReturn(Optional.of(doubleRoom));
		when(roomRepository.existsByRoomNumber("101")).thenReturn(true);

		assertThatExceptionOfType(BusinessRuleException.class)
			.isThrownBy(() -> roomService.create(new RoomRequest(TYPE_ID, " 101 ", 1)))
			.withMessage("La chambre 101 existe déjà");
		verify(roomRepository, never()).save(any());
	}

	@Test
	void roomOfUnknownTypeIsNotFound() {
		when(roomTypeRepository.findById(TYPE_ID)).thenReturn(Optional.empty());

		assertThatExceptionOfType(ResourceNotFoundException.class)
			.isThrownBy(() -> roomService.create(new RoomRequest(TYPE_ID, "101", 1)))
			.withMessage("Type de chambre introuvable : " + TYPE_ID);
	}

	@Test
	void updatesRoomKeepingItsNumber() {
		UUID id = UUID.randomUUID();
		Room room = new Room(doubleRoom, "101", 1);
		when(roomRepository.findById(id)).thenReturn(Optional.of(room));
		when(roomTypeRepository.findById(TYPE_ID)).thenReturn(Optional.of(doubleRoom));

		roomService.update(id, new RoomRequest(TYPE_ID, "101", 2));

		assertThat(room.getFloor()).isEqualTo(2);
	}

	@Test
	void changeStatusOfUnknownRoomIsNotFound() {
		UUID id = UUID.randomUUID();
		when(roomRepository.findById(id)).thenReturn(Optional.empty());

		assertThatExceptionOfType(ResourceNotFoundException.class)
			.isThrownBy(() -> roomService.changeStatus(id, RoomStatus.CLEANING));
	}

}
