package com.pms.hotel.invoice;

import static com.pms.hotel.TestAuthentication.RECEPTIONIST_ID;
import static com.pms.hotel.TestAuthentication.receptionist;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.pms.hotel.common.ResourceNotFoundException;
import com.pms.hotel.config.SecurityConfig;

@WebMvcTest(InvoiceController.class)
@Import(SecurityConfig.class)
class InvoiceControllerTest {

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private InvoiceService invoiceService;

	@Test
	void requiresAuthentication() {
		assertThat(mvc.get().uri("/api/invoices")).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void invoiceOfAReservationIsFoundByItsId() {
		UUID reservationId = UUID.randomUUID();
		when(invoiceService.search(eq(reservationId), isNull(), any())).thenReturn(Page.empty());

		assertThat(mvc.get().uri("/api/invoices?reservationId={id}", reservationId).with(receptionist())).hasStatusOk();
		verify(invoiceService).search(eq(reservationId), isNull(), any());
	}

	@Test
	void detailShowsAmountsBalanceAndReservation() {
		UUID id = UUID.randomUUID();
		when(invoiceService.findById(id)).thenReturn(sample(id, InvoiceStatus.UNPAID));

		assertThat(mvc.get().uri("/api/invoices/{id}", id).with(receptionist())).hasStatusOk()
			.bodyJson()
			.isLenientlyEqualTo("""
					{"status": "UNPAID", "amountTotal": 179.80, "balance": 179.80,
					 "reservation": {"clientName": "Jean Dupont", "roomNumber": "101"}, "payments": []}
					""");
	}

	@Test
	void unknownInvoiceIsNotFound() {
		UUID id = UUID.randomUUID();
		when(invoiceService.findById(id)).thenThrow(new ResourceNotFoundException("Facture", id));

		assertThat(mvc.get().uri("/api/invoices/{id}", id).with(receptionist())).hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void receptionistRecordsAPaymentAndIsItsAuthor() {
		UUID id = UUID.randomUUID();
		when(invoiceService.recordPayment(eq(id), any(), eq(RECEPTIONIST_ID)))
			.thenReturn(sample(id, InvoiceStatus.PARTIALLY_PAID));

		assertThat(pay(id, """
				{"amount": 50.00, "method": "CASH"}
				""")).hasStatus(HttpStatus.CREATED).bodyJson().extractingPath("$.status").isEqualTo("PARTIALLY_PAID");
	}

	@ParameterizedTest
	@ValueSource(strings = { "{\"amount\": 0, \"method\": \"CASH\"}", "{\"amount\": 10.555, \"method\": \"CARD\"}",
			"{\"amount\": 10}", "{\"amount\": 10, \"method\": \"CHEQUE\"}" })
	void invalidPaymentIsABadRequest(String body) {
		assertThat(pay(UUID.randomUUID(), body)).hasStatus(HttpStatus.BAD_REQUEST);
		verifyNoInteractions(invoiceService);
	}

	@Test
	void simultaneousModificationIsAConflict() {
		UUID id = UUID.randomUUID();
		when(invoiceService.recordPayment(eq(id), any(), any()))
			.thenThrow(new ObjectOptimisticLockingFailureException(Invoice.class, id));

		assertThat(pay(id, """
				{"amount": 50.00, "method": "CASH"}
				""")).hasStatus(HttpStatus.CONFLICT)
			.bodyJson()
			.extractingPath("$.detail")
			.isEqualTo("Ces données viennent d'être modifiées par quelqu'un d'autre : rechargez-les et réessayez");
	}

	private MvcTestResult pay(UUID invoiceId, String body) {
		return mvc.post()
			.uri("/api/invoices/{id}/payments", invoiceId)
			.with(receptionist())
			.contentType(MediaType.APPLICATION_JSON)
			.content(body)
			.exchange();
	}

	private static InvoiceResponse sample(UUID id, InvoiceStatus status) {
		return new InvoiceResponse(id, status, new BigDecimal("179.80"), new BigDecimal("0.00"),
				new BigDecimal("179.80"), Instant.EPOCH, new InvoiceResponse.ReservationSummary(UUID.randomUUID(),
						LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 12), "Jean Dupont", "101"),
				List.of());
	}

}
