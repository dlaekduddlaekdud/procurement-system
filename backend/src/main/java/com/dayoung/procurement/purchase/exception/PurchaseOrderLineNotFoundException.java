package com.dayoung.procurement.purchase.exception;

public class PurchaseOrderLineNotFoundException extends RuntimeException {

	public PurchaseOrderLineNotFoundException(Long orderId, Long lineId) {
		super("발주서 %d에서 품목 라인 %d을(를) 찾을 수 없습니다.".formatted(orderId, lineId));
	}
}
