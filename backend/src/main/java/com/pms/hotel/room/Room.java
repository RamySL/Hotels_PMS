package com.pms.hotel.room;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "rooms")
public class Room {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "room_type_id")
	private RoomType roomType;

	private String roomNumber;

	private int floor;

	@Enumerated(EnumType.STRING)
	private RoomStatus status;

	protected Room() {
		// requis par JPA
	}

	public Room(RoomType roomType, String roomNumber, int floor) {
		this.roomType = roomType;
		this.roomNumber = roomNumber.trim();
		this.floor = floor;
		this.status = RoomStatus.READY;
	}

	public UUID getId() {
		return id;
	}

	public RoomType getRoomType() {
		return roomType;
	}

	public String getRoomNumber() {
		return roomNumber;
	}

	public int getFloor() {
		return floor;
	}

	public RoomStatus getStatus() {
		return status;
	}

}
