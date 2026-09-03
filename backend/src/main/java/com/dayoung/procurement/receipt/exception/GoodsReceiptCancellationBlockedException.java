package com.dayoung.procurement.receipt.exception;

public class GoodsReceiptCancellationBlockedException extends RuntimeException {

	public GoodsReceiptCancellationBlockedException(Long purchaseOrderLineId) {
		super("연결된 유효 송장 수량보다 입고 수량이 적어지므로 취소할 수 없습니다. purchaseOrderLineId=%d"
				.formatted(purchaseOrderLineId));
	}
}
