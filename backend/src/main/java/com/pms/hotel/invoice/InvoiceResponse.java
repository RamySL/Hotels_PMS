package com.pms.hotel.invoice;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** balance : positif, reste à payer ; négatif, trop-perçu. */
public record InvoiceResponse(UUID id, InvoiceStatus status, BigDecimal amountTotal, BigDecimal amountPaid,
		BigDecimal balance, Instant issuedAt, ReservationSummary reservation, List<PaymentResponse> payments) {

	static InvoiceResponse from(Invoice invoice) {
		var reservation = invoice.getReservation();
		var client = reservation.getClient();
		return new InvoiceResponse(invoice.getId(), invoice.getStatus(), invoice.getAmountTotal(),
				invoice.getAmountPaid(), invoice.balance(), invoice.getIssuedAt(),
				new ReservationSummary(reservation.getId(), reservation.getCheckInDate(), reservation.getCheckOutDate(),
						client.getFirstName() + " " + client.getLastName(), reservation.getRoom().getRoomNumber()),
				invoice.getPayments().stream().map(PaymentResponse::from).toList());
	}

	public record ReservationSummary(UUID id, LocalDate checkInDate, LocalDate checkOutDate, String clientName,
			String roomNumber) {
	}

	public record PaymentResponse(UUID id, BigDecimal amount, PaymentMethod method, Instant paidAt, String recordedBy) {

		static PaymentResponse from(Payment payment) {
			var user = payment.getRecordedBy();
			return new PaymentResponse(payment.getId(), payment.getAmount(), payment.getPaymentMethod(),
					payment.getPaidAt(), user.getFirstName() + " " + user.getLastName());
		}

	}

}
