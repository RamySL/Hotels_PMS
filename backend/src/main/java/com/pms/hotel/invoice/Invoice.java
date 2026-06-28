package com.pms.hotel.invoice;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.pms.hotel.reservation.Reservation;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/** Facture d'une réservation. Ses paiements ne s'enregistrent qu'à travers elle. */
@Entity
@Table(name = "invoices")
public class Invoice {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "reservation_id")
	private Reservation reservation;

	private BigDecimal amountTotal;

	private BigDecimal amountPaid;

	@Enumerated(EnumType.STRING)
	private InvoiceStatus status;

	private Instant issuedAt;

	/** Verrouillage optimiste : deux encaissements simultanés ne peuvent pas s'écraser. */
	@Version
	private long version;

	@OneToMany(mappedBy = "invoice", cascade = CascadeType.PERSIST)
	@OrderBy("paidAt")
	private List<Payment> payments = new ArrayList<>();

	protected Invoice() {
		// requis par JPA
	}

	/** Facture émise à la réservation, pour le prix du séjour, sans aucun paiement. */
	public Invoice(Reservation reservation, Instant issuedAt) {
		this.reservation = reservation;
		this.amountTotal = reservation.getTotalPrice();
		this.amountPaid = BigDecimal.ZERO;
		this.status = InvoiceStatus.UNPAID;
		this.issuedAt = issuedAt;
	}

	/** Le montant suit le prix de la réservation, et le statut est recalculé. */
	public void updateTotal(BigDecimal amountTotal) {
		this.amountTotal = amountTotal;
		refreshStatus();
	}

	/** Réservation annulée : la facture n'est plus due, mais ses paiements restent visibles. */
	public void cancel() {
		this.status = InvoiceStatus.CANCELLED;
	}

	/**
	 * Reste à payer : positif s'il manque de l'argent, négatif en cas de trop-perçu. Une facture
	 * annulée n'est plus due : tout ce qui a été payé devient un trop-perçu.
	 */
	public BigDecimal balance() {
		BigDecimal due = status == InvoiceStatus.CANCELLED ? BigDecimal.ZERO : amountTotal;
		return due.subtract(amountPaid);
	}

	/** Le statut se déduit des montants ; une facture annulée le reste. */
	private void refreshStatus() {
		if (status == InvoiceStatus.CANCELLED) {
			return;
		}
		if (amountPaid.signum() == 0) {
			status = InvoiceStatus.UNPAID;
		}
		else if (amountPaid.compareTo(amountTotal) < 0) {
			status = InvoiceStatus.PARTIALLY_PAID;
		}
		else {
			status = InvoiceStatus.PAID;
		}
	}

	public UUID getId() {
		return id;
	}

	public Reservation getReservation() {
		return reservation;
	}

	public BigDecimal getAmountTotal() {
		return amountTotal;
	}

	public BigDecimal getAmountPaid() {
		return amountPaid;
	}

	public InvoiceStatus getStatus() {
		return status;
	}

	public Instant getIssuedAt() {
		return issuedAt;
	}

	public List<Payment> getPayments() {
		return Collections.unmodifiableList(payments);
	}

}
