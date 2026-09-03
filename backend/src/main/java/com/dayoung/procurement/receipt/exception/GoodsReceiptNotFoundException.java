package com.dayoung.procurement.receipt.exception;

public class GoodsReceiptNotFoundException extends RuntimeException {

	public GoodsReceiptNotFoundException(Long receiptId) {
		super("입고 문서를 찾을 수 없습니다. receiptId=%d".formatted(receiptId));
	}
}
