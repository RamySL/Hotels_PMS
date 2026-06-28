package com.pms.hotel.invoice;

import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

	private final InvoiceService invoiceService;

	public InvoiceController(InvoiceService invoiceService) {
		this.invoiceService = invoiceService;
	}

	/** ?reservationId= donne la facture d'une réservation ; ?status= filtre la liste. */
	@GetMapping
	public PagedModel<InvoiceResponse> search(@RequestParam(required = false) UUID reservationId,
			@RequestParam(required = false) InvoiceStatus status, @PageableDefault(size = 20) Pageable pageable) {
		return new PagedModel<>(invoiceService.search(reservationId, status, pageable));
	}

	@GetMapping("/{id}")
	public InvoiceResponse findById(@PathVariable UUID id) {
		return invoiceService.findById(id);
	}

}
