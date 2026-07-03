package com.pms.hotel.reservation;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class AvailabilityController {

	private final AvailabilityService availabilityService;

	public AvailabilityController(AvailabilityService availabilityService) {
		this.availabilityService = availabilityService;
	}

	/**
	 * Chambres libres sur [checkIn, checkOut), hors chambres hors service, avec le prix
	 * du séjour.
	 */
	@GetMapping("/api/rooms/availability")
	public List<AvailableRoomResponse> findAvailableRooms(@RequestParam LocalDate checkIn,
			@RequestParam LocalDate checkOut) {
		if (!checkOut.isAfter(checkIn)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"La date de départ doit être postérieure à la date d'arrivée");
		}
		return availabilityService.findAvailableRoomsWithPrice(checkIn, checkOut);
	}

}
