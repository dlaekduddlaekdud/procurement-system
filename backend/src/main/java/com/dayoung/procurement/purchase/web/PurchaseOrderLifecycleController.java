package com.dayoung.procurement.purchase.web;

import com.dayoung.procurement.common.api.ApiResponse;
import com.dayoung.procurement.purchase.application.PurchaseOrderService;
import com.dayoung.procurement.security.AuthenticatedUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/purchase-orders")
public class PurchaseOrderLifecycleController {

	private final PurchaseOrderService purchaseOrderService;

	public PurchaseOrderLifecycleController(PurchaseOrderService purchaseOrderService) {
		this.purchaseOrderService = purchaseOrderService;
	}

	@PostMapping("/{orderId}/send")
	@PreAuthorize("hasRole('BUYER')")
	public ApiResponse<Void> send(
			@AuthenticationPrincipal AuthenticatedUser user,
			@PathVariable Long orderId
	) {
		purchaseOrderService.send(orderId, user.getUserId());
		return ApiResponse.success(null);
	}
}
