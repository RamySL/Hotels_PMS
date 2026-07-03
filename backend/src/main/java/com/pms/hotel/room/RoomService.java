package com.pms.hotel.room;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pms.hotel.common.BusinessRuleException;
import com.pms.hotel.common.ResourceNotFoundException;

@Service
public class RoomService {

	private final RoomRepository roomRepository;

	private final RoomTypeRepository roomTypeRepository;

	public RoomService(RoomRepository roomRepository, RoomTypeRepository roomTypeRepository) {
		this.roomRepository = roomRepository;
		this.roomTypeRepository = roomTypeRepository;
	}

	@Transactional(readOnly = true)
	public List<RoomResponse> findAll(RoomStatus status) {
		List<Room> rooms = status == null ? roomRepository.findAllByOrderByFloorAscRoomNumberAsc()
				: roomRepository.findByStatusOrderByFloorAscRoomNumberAsc(status);
		return rooms.stream().map(RoomResponse::from).toList();
	}

	@Transactional
	public RoomResponse create(RoomRequest request) {
		RoomType roomType = findRoomType(request.roomTypeId());
		String roomNumber = Room.normalizeRoomNumber(request.roomNumber());
		if (roomRepository.existsByRoomNumber(roomNumber)) {
			throw duplicateRoomNumber(roomNumber);
		}
		Room room = new Room(roomType, roomNumber, request.floor());
		return RoomResponse.from(roomRepository.save(room));
	}

	@Transactional
	public RoomResponse update(UUID id, RoomRequest request) {
		Room room = findRoom(id);
		RoomType roomType = findRoomType(request.roomTypeId());
		String roomNumber = Room.normalizeRoomNumber(request.roomNumber());
		if (roomRepository.existsByRoomNumberAndIdNot(roomNumber, id)) {
			throw duplicateRoomNumber(roomNumber);
		}
		room.update(roomType, roomNumber, request.floor());
		return RoomResponse.from(room);
	}

	@Transactional
	public RoomResponse changeStatus(UUID id, RoomStatus status) {
		Room room = findRoom(id);
		room.changeStatus(status);
		return RoomResponse.from(room);
	}

	private Room findRoom(UUID id) {
		return roomRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Chambre", id));
	}

	private RoomType findRoomType(UUID id) {
		return roomTypeRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Type de chambre", id));
	}

	private static BusinessRuleException duplicateRoomNumber(String roomNumber) {
		return new BusinessRuleException("La chambre " + roomNumber + " existe déjà");
	}

}
