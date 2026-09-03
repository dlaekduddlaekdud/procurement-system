package com.dayoung.procurement.purchase.exception;

public class PurchaseVendorNotFoundException extends RuntimeException {

	public PurchaseVendorNotFoundException(Long vendorId) {
		super("활성 상태의 공급업체를 찾을 수 없습니다. ID: " + vendorId);
	}
}
