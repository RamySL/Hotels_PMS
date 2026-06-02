package com.pms.hotel.room;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
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
class RoomTypeServiceTest {

	@Mock
	private RoomTypeRepository roomTypeRepository;

	@InjectMocks
	private RoomTypeService roomTypeService;

	@Test
	void createsRoomTypeWithTrimmedName() {
		when(roomTypeRepository.save(any(RoomType.class))).thenAnswer(invocation -> invocation.getArgument(0));

		RoomTypeResponse created = roomTypeService.create(request(" Suite "));

		assertThat(created.name()).isEqualTo("Suite");
		assertThat(created.basePrice()).isEqualByComparingTo("150.00");
		assertThat(created.capacity()).isEqualTo(2);
	}

	@Test
	void refusesExistingNameOnCreate() {
		when(roomTypeRepository.existsByNameIgnoreCase("Suite")).thenReturn(true);

		assertThatExceptionOfType(BusinessRuleException.class)
			.isThrownBy(() -> roomTypeService.create(request(" Suite ")))
			.withMessage("Un type de chambre nommé « Suite » existe déjà");
		verify(roomTypeRepository, never()).save(any());
	}

	@Test
	void updatesExistingRoomType() {
		UUID id = UUID.randomUUID();
		RoomType doubleRoom = new RoomType("Double", null, new BigDecimal("89.90"), 2);
		when(roomTypeRepository.findById(id)).thenReturn(Optional.of(doubleRoom));

		RoomTypeResponse updated = roomTypeService.update(id,
				new RoomTypeRequest("Double confort", "Vue mer", new BigDecimal("109.00"), 3));

		assertThat(updated.name()).isEqualTo("Double confort");
		assertThat(doubleRoom.getDescription()).isEqualTo("Vue mer");
		assertThat(doubleRoom.getBasePrice()).isEqualByComparingTo("109.00");
		assertThat(doubleRoom.getCapacity()).isEqualTo(3);
	}

	@Test
	void refusesNameOfAnotherTypeOnUpdate() {
		UUID id = UUID.randomUUID();
		when(roomTypeRepository.findById(id))
			.thenReturn(Optional.of(new RoomType("Double", null, new BigDecimal("89.90"), 2)));
		when(roomTypeRepository.existsByNameIgnoreCaseAndIdNot("Suite", id)).thenReturn(true);

		assertThatExceptionOfType(BusinessRuleException.class)
			.isThrownBy(() -> roomTypeService.update(id, request("Suite")));
	}

	@Test
	void updateOfUnknownTypeIsNotFound() {
		UUID id = UUID.randomUUID();
		when(roomTypeRepository.findById(id)).thenReturn(Optional.empty());

		assertThatExceptionOfType(ResourceNotFoundException.class)
			.isThrownBy(() -> roomTypeService.update(id, request("Suite")))
			.withMessage("Type de chambre introuvable : " + id);
	}

	private static RoomTypeRequest request(String name) {
		return new RoomTypeRequest(name, null, new BigDecimal("150.00"), 2);
	}

}
