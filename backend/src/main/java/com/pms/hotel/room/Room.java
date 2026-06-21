package com.pms.hotel.room;

import java.util.UUID;

import com.pms.hotel.common.BusinessRuleException;

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
		update(roomType, roomNumber, floor);
		this.status = RoomStatus.READY;
	}

	/**
	 * Numéro sans espaces superflus : c'est sous cette forme qu'il est stocké, et donc comparé
	 * pour vérifier l'unicité.
	 */
	public static String normalizeRoomNumber(String roomNumber) {
		return roomNumber.trim();
	}

	public void update(RoomType roomType, String roomNumber, int floor) {
		this.roomType = roomType;
		this.roomNumber = normalizeRoomNumber(roomNumber);
		this.floor = floor;
	}

	/**
	 * Changement manuel de statut par le personnel (ménage fait, travaux…). OCCUPIED n'est
	 * posé et levé que par le check-in et le check-out.
	 */
	public void changeStatus(RoomStatus newStatus) {
		if (newStatus == RoomStatus.OCCUPIED) {
			throw new BusinessRuleException("Le statut OCCUPIED est posé par le check-in, pas manuellement");
		}
		if (status == RoomStatus.OCCUPIED) {
			throw new BusinessRuleException(
					"La chambre " + roomNumber + " est occupée : son statut changera au check-out");
		}
		this.status = newStatus;
	}

	/** Arrivée d'un client : la chambre doit être prête, ménage fait. */
	public void checkIn() {
		if (status != RoomStatus.READY) {
			throw new BusinessRuleException("La chambre " + roomNumber + " n'est pas prête (" + status + ")");
		}
		this.status = RoomStatus.OCCUPIED;
	}

	/** Départ du client : la chambre est à nettoyer. */
	public void checkOut() {
		this.status = RoomStatus.CLEANING;
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
