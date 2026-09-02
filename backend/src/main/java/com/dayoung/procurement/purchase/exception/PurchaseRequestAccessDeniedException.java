package com.dayoung.procurement.purchase.exception;

public class PurchaseRequestAccessDeniedException extends RuntimeException {

	public PurchaseRequestAccessDeniedException(Long requestId) {
		super("본인의 구매요청만 처리할 수 있습니다. requestId=%d".formatted(requestId));
	}
}
