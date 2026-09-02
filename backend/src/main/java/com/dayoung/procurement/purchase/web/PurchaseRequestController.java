package com.dayoung.procurement.purchase.web;

import com.dayoung.procurement.common.api.ApiResponse;
import com.dayoung.procurement.purchase.application.PurchaseRequestService;
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
@RequestMapping("/api/purchase-requests")
public class PurchaseRequestController {

	private final PurchaseRequestService purchaseRequestService;

	public PurchaseRequestController(PurchaseRequestService purchaseRequestService) {
		this.purchaseRequestService = purchaseRequestService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('REQUESTER')")
	public ApiResponse<CreatedPurchaseRequestResponse> create(
			@AuthenticationPrincipal AuthenticatedUser user,
			@Valid @RequestBody CreatePurchaseRequestRequest request
	) {
		Long requestId = purchaseRequestService.create(user.getUserId(), request.toCommand());
		return ApiResponse.success(new CreatedPurchaseRequestResponse(requestId));
	}

	@PostMapping("/{requestId}/submit")
	@PreAuthorize("hasRole('REQUESTER')")
	public ApiResponse<Void> submit(
			@AuthenticationPrincipal AuthenticatedUser user,
			@PathVariable Long requestId
	) {
		purchaseRequestService.submit(requestId, user.getUserId());
		return ApiResponse.success(null);
	}

	@PostMapping("/{requestId}/approve")
	@PreAuthorize("hasRole('BUYER')")
	public ApiResponse<Void> approve(
			@AuthenticationPrincipal AuthenticatedUser user,
			@PathVariable Long requestId
	) {
		purchaseRequestService.approve(requestId, user.getUserId());
		return ApiResponse.success(null);
	}

	@PostMapping("/{requestId}/reject")
	@PreAuthorize("hasRole('BUYER')")
	public ApiResponse<Void> reject(
			@AuthenticationPrincipal AuthenticatedUser user,
			@PathVariable Long requestId,
			@Valid @RequestBody RejectPurchaseRequestRequest request
	) {
		purchaseRequestService.reject(requestId, user.getUserId(), request.reason());
		return ApiResponse.success(null);
	}
}
