package com.dayoung.procurement.purchase.exception;

public class PurchaseWarehouseNotFoundException extends RuntimeException {

	public PurchaseWarehouseNotFoundException(Long warehouseId) {
		super("활성 상태의 창고를 찾을 수 없습니다. ID: " + warehouseId);
	}
}
