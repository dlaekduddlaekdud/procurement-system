package com.dayoung.procurement.receipt.exception;

import java.math.BigDecimal;

public class PurchaseOrderQuantityExceededException extends RuntimeException {

	public PurchaseOrderQuantityExceededException(
			Long purchaseOrderLineId,
			BigDecimal orderedQuantity,
			BigDecimal requestedCumulativeQuantity
	) {
		super("발주 라인 %d의 발주수량 %s을 초과하여 누적 %s을 입고할 수 없습니다."
				.formatted(purchaseOrderLineId, orderedQuantity, requestedCumulativeQuantity));
	}
}
