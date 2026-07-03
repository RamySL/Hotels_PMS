package com.pms.hotel.room;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, UUID> {

	// Les listes affichent le type de chaque chambre : on le charge dans la même requête (jointure)
	// plutôt qu'une requête par chambre (N+1)
	@EntityGraph(attributePaths = "roomType")
	List<Room> findAllByOrderByFloorAscRoomNumberAsc();

	@EntityGraph(attributePaths = "roomType")
	List<Room> findByStatusOrderByFloorAscRoomNumberAsc(RoomStatus status);

	boolean existsByRoomNumber(String roomNumber);

	boolean existsByRoomNumberAndIdNot(String roomNumber, UUID id);

	long countByStatusNot(RoomStatus status);

}
