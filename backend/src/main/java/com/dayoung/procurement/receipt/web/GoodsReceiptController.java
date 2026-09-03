package com.dayoung.procurement.receipt.web;

import com.dayoung.procurement.common.api.ApiResponse;
import com.dayoung.procurement.receipt.application.GoodsReceiptService;
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
@RequestMapping("/api/purchase-orders/{orderId}/goods-receipts")
public class GoodsReceiptController {

	private final GoodsReceiptService goodsReceiptService;

	public GoodsReceiptController(GoodsReceiptService goodsReceiptService) {
		this.goodsReceiptService = goodsReceiptService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('BUYER')")
	public ApiResponse<CreatedGoodsReceiptResponse> create(
			@AuthenticationPrincipal AuthenticatedUser user,
			@PathVariable Long orderId,
			@Valid @RequestBody CreateGoodsReceiptRequest request
	) {
		Long receiptId = goodsReceiptService.create(orderId, user.getUserId(), request.toCommand());
		return ApiResponse.success(new CreatedGoodsReceiptResponse(receiptId));
	}
}
