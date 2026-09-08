package com.pms.hotel.demo;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.pms.hotel.client.Client;
import com.pms.hotel.client.ClientRepository;
import com.pms.hotel.invoice.Invoice;
import com.pms.hotel.invoice.InvoiceRepository;
import com.pms.hotel.invoice.PaymentMethod;
import com.pms.hotel.reservation.Reservation;
import com.pms.hotel.reservation.ReservationRepository;
import com.pms.hotel.room.Room;
import com.pms.hotel.room.RoomRepository;
import com.pms.hotel.room.RoomStatus;
import com.pms.hotel.room.RoomType;
import com.pms.hotel.room.RoomTypeRepository;
import com.pms.hotel.user.User;
import com.pms.hotel.user.UserRepository;

/**
 * Données de démonstration, créées au démarrage avec le profil demo (docker-compose) si
 * l'hôtel n'a encore aucune chambre. Les dates partent d'aujourd'hui : arrivées, départs
 * et retards du jour, séjours en cours, passés et à venir, factures dans chaque statut.
 * <p>
 * Les séjours commencés avant aujourd'hui passent par les règles du domaine, pas par le
 * service, qui refuse une arrivée dans le passé. La base garde sa contrainte
 * anti-chevauchement (ADR-006).
 */
@Component
@Profile("demo")
public class DemoData implements ApplicationRunner {

	private static final Logger LOGGER = LoggerFactory.getLogger(DemoData.class);

	private final RoomTypeRepository roomTypeRepository;

	private final RoomRepository roomRepository;

	private final ClientRepository clientRepository;

	private final ReservationRepository reservationRepository;

	private final InvoiceRepository invoiceRepository;

	private final UserRepository userRepository;

	private final Clock clock;

	public DemoData(RoomTypeRepository roomTypeRepository, RoomRepository roomRepository,
			ClientRepository clientRepository, ReservationRepository reservationRepository,
			InvoiceRepository invoiceRepository, UserRepository userRepository, Clock clock) {
		this.roomTypeRepository = roomTypeRepository;
		this.roomRepository = roomRepository;
		this.clientRepository = clientRepository;
		this.reservationRepository = reservationRepository;
		this.invoiceRepository = invoiceRepository;
		this.userRepository = userRepository;
		this.clock = clock;
	}

	/** Ne crée rien si l'hôtel a déjà des chambres : un redémarrage garde les données. */
	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (roomRepository.count() > 0) {
			LOGGER.info("Données de démo déjà présentes : rien à créer");
			return;
		}
		User reception = userRepository.findByEmail("reception@pms.local").orElseThrow();
		new Seeding(LocalDate.now(clock), reception).create();
		LOGGER.info("Données de démo créées : {} chambres, {} clients, {} réservations", roomRepository.count(),
				clientRepository.count(), reservationRepository.count());
	}

	/** Une création des données, datée à partir du jour de l'hôtel. */
	private final class Seeding {

		private final LocalDate today;

		private final User reception;

		private final Map<String, Room> rooms = new HashMap<>();

		private final Map<String, Client> clients = new HashMap<>();

		Seeding(LocalDate today, User reception) {
			this.today = today;
			this.reception = reception;
		}

		void create() {
			createRooms();
			createClients();
			createStays();
		}

		private void createRooms() {
			RoomType simpleType = roomTypeRepository
				.save(new RoomType("Simple", "Lit de 90, idéale pour un voyageur seul", new BigDecimal("59.00"), 1));
			RoomType doubleType = roomTypeRepository
				.save(new RoomType("Double", "Lit de 160 ou deux lits jumeaux", new BigDecimal("89.90"), 2));
			RoomType familyType = roomTypeRepository
				.save(new RoomType("Familiale", "Un lit double et deux lits simples", new BigDecimal("129.00"), 4));
			RoomType suiteType = roomTypeRepository
				.save(new RoomType("Suite", "Salon séparé, vue sur le port", new BigDecimal("189.00"), 4));
			room(doubleType, "101", 1);
			room(doubleType, "102", 1);
			room(simpleType, "103", 1);
			room(simpleType, "104", 1);
			room(familyType, "105", 1);
			room(doubleType, "201", 2);
			room(doubleType, "202", 2);
			room(suiteType, "203", 2);
			room(familyType, "204", 2);
			// En travaux : la disponibilité l'écarte
			room(doubleType, "205", 2).changeStatus(RoomStatus.OUT_OF_SERVICE);
		}

		private Room room(RoomType type, String roomNumber, int floor) {
			Room room = roomRepository.save(new Room(type, roomNumber, floor));
			rooms.put(roomNumber, room);
			return room;
		}

		private void createClients() {
			client("Hélène", "Dupont", "helene.dupont@example.com", "06 12 34 56 78");
			client("Jean", "Martin", null, null);
			client("Paul", "Durand", "paul.durand@example.com", null);
			client("Léa", "Petit", null, "07 98 76 54 32");
			client("Marc", "Leroy", null, null);
			client("Sophie", "Bernard", "sophie.bernard@example.com", "06 45 23 87 10");
			client("Claire", "Moreau", "claire.moreau@example.com", null);
			client("Thomas", "Lefèvre", null, "06 70 11 22 33");
			client("Camille", "Girard", "camille.girard@example.com", null);
			client("Nicolas", "Fournier", null, null);
			client("Julie", "Mercier", "julie.mercier@example.com", "07 61 52 43 34");
			client("Antoine", "Garnier", "antoine.garnier@example.com", null);
		}

		private void client(String firstName, String lastName, String email, String phone) {
			clients.put(lastName, clientRepository.save(new Client(firstName, lastName, email, phone)));
		}

		/**
		 * Quinze séjours, sans chevauchement dans une même chambre. Les jours sont des
		 * écarts avec aujourd'hui : book("Lefèvre", "102", -9, 3, 2, -30) arrive il y a 9
		 * jours pour 3 nuits et 2 personnes, et a réservé il y a 30 jours.
		 */
		private void createStays() {
			// Séjours passés, réglés au départ ; leurs chambres ont été nettoyées depuis
			Stay lefevre = book("Lefèvre", "102", -9, 3, 2, -30);
			checkIn(lefevre, -9);
			pay(lefevre, "269.70", PaymentMethod.CARD, -6);
			checkOut(lefevre, true);
			Stay girard = book("Girard", "203", -8, 3, 3, -40);
			pay(girard, "150.00", PaymentMethod.TRANSFER, -35);
			checkIn(girard, -8);
			pay(girard, "417.00", PaymentMethod.CARD, -5);
			checkOut(girard, true);

			// Partis ce matin, soldés : leurs chambres sont à nettoyer
			Stay mercier = book("Mercier", "105", -2, 2, 4, -12);
			checkIn(mercier, -2);
			pay(mercier, "258.00", PaymentMethod.CASH, 0);
			checkOut(mercier, false);
			Stay garnier = book("Garnier", "201", -3, 3, 2, -20);
			checkIn(garnier, -3);
			pay(garnier, "269.70", PaymentMethod.CARD, 0);
			checkOut(garnier, false);

			// En séjour : acompte versé, départ aujourd'hui non réglé, départ oublié hier
			Stay martin = book("Martin", "101", -2, 4, 2, -15);
			checkIn(martin, -2);
			pay(martin, "100.00", PaymentMethod.CARD, -2);
			Stay petit = book("Petit", "103", -2, 2, 1, -6);
			checkIn(petit, -2);
			Stay leroy = book("Leroy", "104", -4, 3, 1, -10);
			checkIn(leroy, -4);
			pay(leroy, "100.00", PaymentMethod.CASH, -4);
			Stay moreau = book("Moreau", "204", -1, 4, 3, -9);
			checkIn(moreau, -1);

			// Arrivées : une attendue aujourd'hui avec un acompte, une attendue hier
			Stay dupont = book("Dupont", "202", 0, 3, 2, -10);
			pay(dupont, "50.00", PaymentMethod.CARD, -10);
			book("Bernard", "102", -1, 2, 2, -7);

			// À venir sur les deux prochaines semaines
			book("Durand", "203", 3, 3, 4, -5);
			book("Fournier", "101", 4, 3, 2, -3);
			book("Dupont", "105", 8, 2, 3, -2);
			book("Mercier", "201", 10, 2, 2, -1);

			// Annulée après un acompte : le trop-perçu est à rembourser
			Stay cancelled = book("Garnier", "202", 5, 2, 2, -4);
			pay(cancelled, "60.00", PaymentMethod.TRANSFER, -4);
			cancelled.reservation().cancel();
			cancelled.invoice().cancel();
		}

		/**
		 * Réservation et sa facture, créées le jour bookedOn ; les écarts partent
		 * d'aujourd'hui.
		 */
		private Stay book(String client, String roomNumber, int checkIn, int nights, int guests, int bookedOn) {
			Instant bookedAt = at(bookedOn, 14);
			Reservation reservation = reservationRepository
				.save(new Reservation(clients.get(client), rooms.get(roomNumber), today.plusDays(checkIn),
						today.plusDays(checkIn + nights), guests, reception, bookedAt));
			Invoice invoice = invoiceRepository.save(new Invoice(reservation, bookedAt));
			return new Stay(reservation, invoice);
		}

		private void checkIn(Stay stay, int day) {
			stay.reservation().checkIn(today.plusDays(day));
		}

		private void pay(Stay stay, String amount, PaymentMethod method, int day) {
			stay.invoice().recordPayment(new BigDecimal(amount), method, reception, at(day, 10));
		}

		/**
		 * Départ du client ; une chambre libérée avant aujourd'hui a été nettoyée depuis.
		 */
		private void checkOut(Stay stay, boolean cleaned) {
			stay.reservation().checkOut();
			if (cleaned) {
				stay.reservation().getRoom().changeStatus(RoomStatus.READY);
			}
		}

		private Instant at(int day, int hour) {
			return today.plusDays(day).atTime(hour, 0).atZone(clock.getZone()).toInstant();
		}

	}

	private record Stay(Reservation reservation, Invoice invoice) {
	}

}
