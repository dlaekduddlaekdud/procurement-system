package com.dayoung.procurement.invoice.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record InvoiceLineAmounts(
		BigDecimal supplyAmount,
		BigDecimal taxAmount,
		BigDecimal totalAmount
) {

	private static final BigDecimal VAT_RATE = new BigDecimal("0.10");

	public static InvoiceLineAmounts calculate(BigDecimal quantity, BigDecimal unitPrice) {
		BigDecimal supplyAmount = quantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
		BigDecimal taxAmount = supplyAmount.multiply(VAT_RATE)
				.setScale(0, RoundingMode.DOWN)
				.setScale(2);
		return new InvoiceLineAmounts(supplyAmount, taxAmount, supplyAmount.add(taxAmount));
	}
}
