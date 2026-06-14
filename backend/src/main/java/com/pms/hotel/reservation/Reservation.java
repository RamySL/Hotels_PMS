package com.pms.hotel.reservation;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import com.pms.hotel.client.Client;
import com.pms.hotel.common.BusinessRuleException;
import com.pms.hotel.room.Room;
import com.pms.hotel.room.RoomType;
import com.pms.hotel.user.User;

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
@Table(name = "reservations")
public class Reservation {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "client_id")
	private Client client;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "room_id")
	private Room room;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "created_by")
	private User createdBy;

	private LocalDate checkInDate;

	private LocalDate checkOutDate;

	private int numberOfGuests;

	private BigDecimal totalPrice;

	@Enumerated(EnumType.STRING)
	private ReservationStatus status;

	private Instant createdAt;

	protected Reservation() {
		// requis par JPA
	}

	/** Une réservation est confirmée dès sa création (pas d'état « en attente » dans le MVP). */
	public Reservation(Client client, Room room, LocalDate checkInDate, LocalDate checkOutDate, int numberOfGuests,
			User createdBy, Instant createdAt) {
		this.client = client;
		this.createdBy = createdBy;
		this.createdAt = createdAt;
		this.status = ReservationStatus.CONFIRMED;
		schedule(room, checkInDate, checkOutDate, numberOfGuests);
	}

	/** Prix du séjour : nombre de nuits × prix de base du type de chambre (ADR-004). */
	public static BigDecimal priceOf(RoomType roomType, LocalDate checkInDate, LocalDate checkOutDate) {
		return roomType.getBasePrice().multiply(BigDecimal.valueOf(nightsBetween(checkInDate, checkOutDate)));
	}

	public long nights() {
		return nightsBetween(checkInDate, checkOutDate);
	}

	private static long nightsBetween(LocalDate checkInDate, LocalDate checkOutDate) {
		return ChronoUnit.DAYS.between(checkInDate, checkOutDate);
	}

	private void schedule(Room room, LocalDate checkInDate, LocalDate checkOutDate, int numberOfGuests) {
		RoomType roomType = room.getRoomType();
		if (numberOfGuests > roomType.getCapacity()) {
			throw new BusinessRuleException("La chambre " + room.getRoomNumber() + " (" + roomType.getName()
					+ ") accueille " + roomType.getCapacity() + " personnes au maximum");
		}
		this.room = room;
		this.checkInDate = checkInDate;
		this.checkOutDate = checkOutDate;
		this.numberOfGuests = numberOfGuests;
		this.totalPrice = priceOf(roomType, checkInDate, checkOutDate);
	}

	public UUID getId() {
		return id;
	}

	public Client getClient() {
		return client;
	}

	public Room getRoom() {
		return room;
	}

	public User getCreatedBy() {
		return createdBy;
	}

	public LocalDate getCheckInDate() {
		return checkInDate;
	}

	public LocalDate getCheckOutDate() {
		return checkOutDate;
	}

	public int getNumberOfGuests() {
		return numberOfGuests;
	}

	public BigDecimal getTotalPrice() {
		return totalPrice;
	}

	public ReservationStatus getStatus() {
		return status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
