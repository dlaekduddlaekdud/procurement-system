package com.dayoung.procurement.purchase.exception;

public class PurchaseItemNotFoundException extends RuntimeException {

	public PurchaseItemNotFoundException(Long itemId) {
		super("활성 품목을 찾을 수 없습니다. itemId=%d".formatted(itemId));
	}
}
