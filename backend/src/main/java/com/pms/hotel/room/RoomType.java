package com.pms.hotel.room;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "room_types")
public class RoomType {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	private String name;

	private String description;

	private BigDecimal basePrice;

	private int capacity;

	protected RoomType() {
		// requis par JPA
	}

	public RoomType(String name, String description, BigDecimal basePrice, int capacity) {
		update(name, description, basePrice, capacity);
	}

	/**
	 * Nom sans espaces superflus : c'est sous cette forme qu'il est stocké, et donc comparé
	 * pour vérifier l'unicité.
	 */
	public static String normalizeName(String name) {
		return name.trim();
	}

	public void update(String name, String description, BigDecimal basePrice, int capacity) {
		this.name = normalizeName(name);
		this.description = description;
		this.basePrice = basePrice;
		this.capacity = capacity;
	}

	public UUID getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public BigDecimal getBasePrice() {
		return basePrice;
	}

	public int getCapacity() {
		return capacity;
	}

}
