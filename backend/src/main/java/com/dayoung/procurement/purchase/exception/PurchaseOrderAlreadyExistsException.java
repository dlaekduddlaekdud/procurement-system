package com.dayoung.procurement.purchase.exception;

public class PurchaseOrderAlreadyExistsException extends RuntimeException {

	public PurchaseOrderAlreadyExistsException(Long requestId) {
		super("구매요청 " + requestId + "은(는) 이미 발주서로 전환되었습니다.");
	}
}
