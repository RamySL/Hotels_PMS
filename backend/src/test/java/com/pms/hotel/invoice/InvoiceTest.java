package com.pms.hotel.invoice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.pms.hotel.client.Client;
import com.pms.hotel.common.BusinessRuleException;
import com.pms.hotel.reservation.Reservation;
import com.pms.hotel.room.Room;
import com.pms.hotel.room.RoomType;
import com.pms.hotel.user.Role;
import com.pms.hotel.user.User;

/** Facture de deux nuits en chambre double : 179,80 €. */
class InvoiceTest {

	private static final LocalDate ARRIVAL = LocalDate.of(2026, 10, 10);

	private final User receptionist = new User("reception@pms.local", "hash", "Lucas", "Bernard", Role.RECEPTIONIST);

	private final Invoice invoice = new Invoice(twoNightsInADouble(), Instant.EPOCH);

	@Test
	void newInvoiceIsUnpaidAndOwesThePriceOfTheStay() {
		assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.UNPAID);
		assertThat(invoice.getAmountTotal()).isEqualByComparingTo("179.80");
		assertThat(invoice.balance()).isEqualByComparingTo("179.80");
	}

	@Test
	void totalFollowsTheReservationPrice() {
		invoice.updateTotal(new BigDecimal("269.70"));

		assertThat(invoice.getAmountTotal()).isEqualByComparingTo("269.70");
		assertThat(invoice.balance()).isEqualByComparingTo("269.70");
		assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.UNPAID);
	}

	@Test
	void cancelledInvoiceIsNoLongerDue() {
		invoice.cancel();

		assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.CANCELLED);
		assertThat(invoice.balance()).isEqualByComparingTo("0");
	}

	@Test
	void cancelledInvoiceStaysCancelledWhenItsTotalChanges() {
		invoice.cancel();

		invoice.updateTotal(new BigDecimal("89.90"));

		assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.CANCELLED);
	}

	@Test
	void partialPaymentLeavesABalance() {
		pay("50.00", PaymentMethod.CASH);

		assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PARTIALLY_PAID);
		assertThat(invoice.balance()).isEqualByComparingTo("129.80");
		assertThat(invoice.getPayments()).singleElement().satisfies(payment -> {
			assertThat(payment.getAmount()).isEqualByComparingTo("50.00");
			assertThat(payment.getPaymentMethod()).isEqualTo(PaymentMethod.CASH);
			assertThat(payment.getRecordedBy()).isSameAs(receptionist);
		});
	}

	@Test
	void payingTheBalanceSettlesTheInvoice() {
		pay("50.00", PaymentMethod.CASH);
		pay("129.80", PaymentMethod.CARD);

		assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
		assertThat(invoice.balance()).isEqualByComparingTo("0");
		assertThatCode(invoice::ensureSettled).doesNotThrowAnyException();
	}

	@Test
	void paymentCannotExceedTheBalance() {
		assertThatExceptionOfType(BusinessRuleException.class).isThrownBy(() -> pay("179.81", PaymentMethod.CARD))
			.withMessage("Le paiement dépasse le reste à payer (179,80 €)");
		assertThat(invoice.getPayments()).isEmpty();
	}

	@Test
	void settledInvoiceTakesNoMorePayment() {
		pay("179.80", PaymentMethod.CARD);

		assertThatExceptionOfType(BusinessRuleException.class).isThrownBy(() -> pay("1.00", PaymentMethod.CASH))
			.withMessage("Cette facture est déjà soldée");
	}

	@Test
	void cancelledInvoiceTakesNoPayment() {
		invoice.cancel();

		assertThatExceptionOfType(BusinessRuleException.class).isThrownBy(() -> pay("10.00", PaymentMethod.CASH))
			.withMessage("Facture annulée : aucun paiement ne peut y être enregistré");
	}

	@Test
	void depositPaidBeforeACancellationBecomesAnOverpayment() {
		pay("50.00", PaymentMethod.CASH);

		invoice.cancel();

		assertThat(invoice.balance()).isEqualByComparingTo("-50.00");
	}

	@Test
	void extensionAfterFullPaymentLeavesTheNewNightToPay() {
		pay("179.80", PaymentMethod.CARD);

		invoice.updateTotal(new BigDecimal("269.70"));

		assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PARTIALLY_PAID);
		assertThat(invoice.balance()).isEqualByComparingTo("89.90");
	}

	@Test
	void lowerPriceAfterFullPaymentShowsAnOverpayment() {
		pay("179.80", PaymentMethod.CARD);

		invoice.updateTotal(new BigDecimal("89.90"));

		assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
		assertThat(invoice.balance()).isEqualByComparingTo("-89.90");
	}

	@Test
	void unsettledInvoiceSaysWhatIsLeftToPay() {
		assertThatExceptionOfType(BusinessRuleException.class).isThrownBy(invoice::ensureSettled)
			.withMessage("La facture n'est pas soldée : il reste 179,80 € à payer");
	}

	private void pay(String amount, PaymentMethod method) {
		invoice.recordPayment(new BigDecimal(amount), method, receptionist, Instant.EPOCH);
	}

	private Reservation twoNightsInADouble() {
		Room room101 = new Room(new RoomType("Double", null, new BigDecimal("89.90"), 2), "101", 1);
		return new Reservation(new Client("Jean", "Dupont", null, null), room101, ARRIVAL, ARRIVAL.plusDays(2), 2,
				receptionist, Instant.EPOCH);
	}

}
