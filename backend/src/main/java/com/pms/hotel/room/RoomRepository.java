package com.pms.hotel.room;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, UUID> {

	List<Room> findAllByOrderByFloorAscRoomNumberAsc();

	List<Room> findByStatusOrderByFloorAscRoomNumberAsc(RoomStatus status);

	boolean existsByRoomNumber(String roomNumber);

	boolean existsByRoomNumberAndIdNot(String roomNumber, UUID id);

}
