package com.dayoung.procurement.invoice.web;

import com.dayoung.procurement.common.api.ApiResponse;
import com.dayoung.procurement.invoice.application.InvoiceService;
import com.dayoung.procurement.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/purchase-orders/{orderId}/invoices")
public class InvoiceController {

	private final InvoiceService invoiceService;

	public InvoiceController(InvoiceService invoiceService) {
		this.invoiceService = invoiceService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('BUYER')")
	public ApiResponse<CreatedInvoiceResponse> create(
			@AuthenticationPrincipal AuthenticatedUser user,
			@PathVariable Long orderId,
			@Valid @RequestBody CreateInvoiceRequest request
	) {
		Long invoiceId = invoiceService.create(orderId, user.getUserId(), request.toCommand());
		return ApiResponse.success(new CreatedInvoiceResponse(invoiceId));
	}
}
