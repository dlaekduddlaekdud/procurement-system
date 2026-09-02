package com.dayoung.procurement.purchase.exception;

import com.dayoung.procurement.purchase.domain.PurchaseRequestStatus;

public class InvalidPurchaseRequestStateException extends RuntimeException {

	public InvalidPurchaseRequestStateException(PurchaseRequestStatus currentStatus, String action) {
		super("구매요청 상태가 %s이므로 %s할 수 없습니다.".formatted(currentStatus, action));
	}

	public InvalidPurchaseRequestStateException(String message) {
		super(message);
	}
}
