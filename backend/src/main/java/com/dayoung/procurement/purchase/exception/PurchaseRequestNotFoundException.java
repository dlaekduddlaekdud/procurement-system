package com.dayoung.procurement.purchase.exception;

public class PurchaseRequestNotFoundException extends RuntimeException {

	public PurchaseRequestNotFoundException(Long requestId) {
		super("구매요청을 찾을 수 없습니다. requestId=%d".formatted(requestId));
	}
}
