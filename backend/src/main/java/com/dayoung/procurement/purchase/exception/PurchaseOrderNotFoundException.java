package com.dayoung.procurement.purchase.exception;

public class PurchaseOrderNotFoundException extends RuntimeException {

	public PurchaseOrderNotFoundException(Long orderId) {
		super("발주서를 찾을 수 없습니다. orderId=%d".formatted(orderId));
	}
}
