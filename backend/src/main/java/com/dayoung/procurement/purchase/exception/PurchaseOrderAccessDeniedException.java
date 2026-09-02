package com.dayoung.procurement.purchase.exception;

public class PurchaseOrderAccessDeniedException extends RuntimeException {

	public PurchaseOrderAccessDeniedException(Long orderId) {
		super("담당 발주자만 발주서를 처리할 수 있습니다. orderId=%d".formatted(orderId));
	}
}
