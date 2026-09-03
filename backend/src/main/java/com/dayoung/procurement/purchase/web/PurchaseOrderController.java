package com.dayoung.procurement.purchase.web;

import com.dayoung.procurement.common.api.ApiResponse;
import com.dayoung.procurement.purchase.application.PurchaseOrderService;
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
@RequestMapping("/api/purchase-requests/{requestId}/purchase-order")
public class PurchaseOrderController {

	private final PurchaseOrderService purchaseOrderService;

	public PurchaseOrderController(PurchaseOrderService purchaseOrderService) {
		this.purchaseOrderService = purchaseOrderService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('BUYER')")
	public ApiResponse<CreatedPurchaseOrderResponse> create(
			@AuthenticationPrincipal AuthenticatedUser user,
			@PathVariable Long requestId,
			@Valid @RequestBody CreatePurchaseOrderRequest request
	) {
		Long orderId = purchaseOrderService.createFromApprovedRequest(
				requestId,
				user.getUserId(),
				request.toCommand()
		);
		return ApiResponse.success(new CreatedPurchaseOrderResponse(orderId));
	}
}
