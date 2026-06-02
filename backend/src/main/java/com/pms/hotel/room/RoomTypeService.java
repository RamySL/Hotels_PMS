package com.pms.hotel.room;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pms.hotel.common.BusinessRuleException;
import com.pms.hotel.common.ResourceNotFoundException;

@Service
public class RoomTypeService {

	private final RoomTypeRepository roomTypeRepository;

	public RoomTypeService(RoomTypeRepository roomTypeRepository) {
		this.roomTypeRepository = roomTypeRepository;
	}

	@Transactional(readOnly = true)
	public List<RoomTypeResponse> findAll() {
		return roomTypeRepository.findAllByOrderByNameAsc().stream().map(RoomTypeResponse::from).toList();
	}

	@Transactional
	public RoomTypeResponse create(RoomTypeRequest request) {
		String name = RoomType.normalizeName(request.name());
		if (roomTypeRepository.existsByNameIgnoreCase(name)) {
			throw duplicateName(name);
		}
		RoomType roomType = new RoomType(name, request.description(), request.basePrice(), request.capacity());
		return RoomTypeResponse.from(roomTypeRepository.save(roomType));
	}

	@Transactional
	public RoomTypeResponse update(UUID id, RoomTypeRequest request) {
		RoomType roomType = roomTypeRepository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("Type de chambre", id));
		String name = RoomType.normalizeName(request.name());
		if (roomTypeRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
			throw duplicateName(name);
		}
		roomType.update(name, request.description(), request.basePrice(), request.capacity());
		return RoomTypeResponse.from(roomType);
	}

	private static BusinessRuleException duplicateName(String name) {
		return new BusinessRuleException("Un type de chambre nommé « " + name + " » existe déjà");
	}

}
