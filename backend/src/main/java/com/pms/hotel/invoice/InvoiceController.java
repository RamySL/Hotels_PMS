package com.pms.hotel.invoice;

import java.util.UUID;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/invoices")
@Tag(name = "Facturation")
public class InvoiceController {

	private final InvoiceService invoiceService;

	public InvoiceController(InvoiceService invoiceService) {
		this.invoiceService = invoiceService;
	}

	/** ?reservationId= donne la facture d'une réservation ; ?status= filtre la liste. */
	@GetMapping
	@Operation(summary = "Lister les factures, par réservation ou par statut")
	public PagedModel<InvoiceResponse> search(@RequestParam(required = false) UUID reservationId,
			@RequestParam(required = false) InvoiceStatus status,
			@ParameterObject @PageableDefault(size = 20) Pageable pageable) {
		return new PagedModel<>(invoiceService.search(reservationId, status, pageable));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Détail d'une facture : montants, reste à payer et paiements")
	public InvoiceResponse findById(@PathVariable UUID id) {
		return invoiceService.findById(id);
	}

	/**
	 * Enregistre un paiement et renvoie la facture à jour ; l'auteur est l'utilisateur du
	 * jeton.
	 */
	@PostMapping("/{id}/payments")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Enregistrer un paiement, sans dépasser le reste à payer")
	public InvoiceResponse recordPayment(@PathVariable UUID id, @Valid @RequestBody PaymentRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		return invoiceService.recordPayment(id, request, UUID.fromString(jwt.getSubject()));
	}

}
