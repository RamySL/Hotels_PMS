package com.pms.hotel.invoice;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

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
@Table(name = "payments")
public class Payment {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "invoice_id")
	private Invoice invoice;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "recorded_by")
	private User recordedBy;

	private BigDecimal amount;

	@Enumerated(EnumType.STRING)
	private PaymentMethod paymentMethod;

	private Instant paidAt;

	protected Payment() {
		// requis par JPA
	}

	public UUID getId() {
		return id;
	}

	public Invoice getInvoice() {
		return invoice;
	}

	public User getRecordedBy() {
		return recordedBy;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public PaymentMethod getPaymentMethod() {
		return paymentMethod;
	}

	public Instant getPaidAt() {
		return paidAt;
	}

}
