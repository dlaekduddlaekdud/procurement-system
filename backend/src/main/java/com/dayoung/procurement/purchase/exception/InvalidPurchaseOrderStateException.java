package com.dayoung.procurement.purchase.exception;

import com.dayoung.procurement.purchase.domain.PurchaseOrderStatus;

public class InvalidPurchaseOrderStateException extends RuntimeException {

	public InvalidPurchaseOrderStateException(PurchaseOrderStatus currentStatus, String action) {
		super("발주서 상태가 %s이므로 %s할 수 없습니다.".formatted(currentStatus, action));
	}
}
