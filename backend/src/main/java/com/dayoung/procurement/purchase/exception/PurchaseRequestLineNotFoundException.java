package com.dayoung.procurement.purchase.exception;

public class PurchaseRequestLineNotFoundException extends RuntimeException {

	public PurchaseRequestLineNotFoundException(Long requestId, Long lineId) {
		super("구매요청 " + requestId + "에서 품목 라인 " + lineId + "을(를) 찾을 수 없습니다.");
	}
}
