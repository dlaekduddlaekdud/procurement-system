package com.dayoung.procurement;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dayoung.procurement.invoice.domain.InvoiceLineAmounts;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class InvoiceLineAmountsTest {

	@Test
	void truncatesLineTaxBelowOneWon() {
		InvoiceLineAmounts amounts = InvoiceLineAmounts.calculate(
				new BigDecimal("3.000"),
				new BigDecimal("333.33")
		);

		assertEquals(new BigDecimal("999.99"), amounts.supplyAmount());
		assertEquals(new BigDecimal("99.00"), amounts.taxAmount());
		assertEquals(new BigDecimal("1098.99"), amounts.totalAmount());
	}

	@Test
	void calculatesTaxForEachLineBeforeSumming() {
		InvoiceLineAmounts firstLine = InvoiceLineAmounts.calculate(BigDecimal.ONE, new BigDecimal("15.00"));
		InvoiceLineAmounts secondLine = InvoiceLineAmounts.calculate(BigDecimal.ONE, new BigDecimal("15.00"));

		BigDecimal taxTotal = firstLine.taxAmount().add(secondLine.taxAmount());

		assertEquals(new BigDecimal("2.00"), taxTotal);
	}
}
