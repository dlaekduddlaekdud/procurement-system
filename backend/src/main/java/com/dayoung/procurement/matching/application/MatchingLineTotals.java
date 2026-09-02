package com.dayoung.procurement.matching.application;

import java.math.BigDecimal;

public record MatchingLineTotals(
		Long purchaseOrderLineId,
		BigDecimal orderedQuantity,
		BigDecimal receivedQuantity,
		BigDecimal invoicedQuantity,
		BigDecimal orderedAmount,
		BigDecimal invoicedAmount
) {
}
