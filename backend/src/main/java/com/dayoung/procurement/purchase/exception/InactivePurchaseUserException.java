package com.dayoung.procurement.purchase.exception;

public class InactivePurchaseUserException extends RuntimeException {

	public InactivePurchaseUserException(Long userId) {
		super("활성 사용자를 찾을 수 없습니다. userId=%d".formatted(userId));
	}
}
