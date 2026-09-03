package com.dayoung.procurement.purchase.application;

import java.math.BigDecimal;

public record PurchaseRequestLineDetail(
		Long id,
		int lineNumber,
		Long itemId,
		String itemCode,
		String itemName,
		BigDecimal quantity,
		String unit,
		BigDecimal estimatedUnitPrice,
		BigDecimal estimatedAmount,
		String description
) {
}
