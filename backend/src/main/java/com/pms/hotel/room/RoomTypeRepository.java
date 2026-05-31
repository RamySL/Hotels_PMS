package com.pms.hotel.room;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomTypeRepository extends JpaRepository<RoomType, UUID> {

}
