package com.dayoung.procurement.purchase.web;

import com.dayoung.procurement.common.api.ApiResponse;
import com.dayoung.procurement.purchase.application.PurchaseRequestDetail;
import com.dayoung.procurement.purchase.application.PurchaseRequestService;
import com.dayoung.procurement.purchase.application.PurchaseRequestSummary;
import com.dayoung.procurement.security.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
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

	@GetMapping
	@PreAuthorize("hasRole('REQUESTER')")
	public ApiResponse<List<PurchaseRequestSummary>> findMine(
			@AuthenticationPrincipal AuthenticatedUser user
	) {
		return ApiResponse.success(purchaseRequestService.findMine(user.getUserId()));
	}

	@GetMapping("/{requestId}")
	@PreAuthorize("hasRole('REQUESTER')")
	public ApiResponse<PurchaseRequestDetail> getMine(
			@AuthenticationPrincipal AuthenticatedUser user,
			@PathVariable Long requestId
	) {
		return ApiResponse.success(purchaseRequestService.getMine(requestId, user.getUserId()));
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
