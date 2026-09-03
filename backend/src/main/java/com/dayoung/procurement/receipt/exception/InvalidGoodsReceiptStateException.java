package com.dayoung.procurement.receipt.exception;

import com.dayoung.procurement.receipt.domain.GoodsReceiptStatus;

public class InvalidGoodsReceiptStateException extends RuntimeException {

	public InvalidGoodsReceiptStateException(GoodsReceiptStatus status) {
		super("입고 문서 상태가 %s이므로 요청한 작업을 수행할 수 없습니다.".formatted(status));
	}
}
