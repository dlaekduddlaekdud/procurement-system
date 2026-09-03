package com.dayoung.procurement;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dayoung.procurement.matching.application.MatchingLineTotals;
import com.dayoung.procurement.matching.application.ThreeWayMatchingPolicy;
import com.dayoung.procurement.matching.domain.MatchingStatus;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ThreeWayMatchingPolicyTest {

	private final ThreeWayMatchingPolicy matchingPolicy = new ThreeWayMatchingPolicy();

	@Test
	void matchesWhenAmountDifferenceIsWithinTolerance() {
		assertEquals(MatchingStatus.MATCHED, matchingPolicy.decide(totals("10.000", "10.000", "10000000.00")));
		assertEquals(MatchingStatus.MATCHED, matchingPolicy.decide(totals("10.000", "10.000", "10005000.00")));
		assertEquals(MatchingStatus.MATCHED, matchingPolicy.decide(totals("10.000", "10.000", "10010000.00")));
	}

	@Test
	void holdsQuantityWhenInvoicedQuantityExceedsReceivedQuantity() {
		assertEquals(
				MatchingStatus.HOLD_QUANTITY,
				matchingPolicy.decide(totals("8.000", "10.000", "10050000.00"))
		);
	}

	@Test
	void holdsQuantityWhenNoInvoiceIsReceived() {
		assertEquals(
				MatchingStatus.HOLD_QUANTITY,
				matchingPolicy.decide(totals("10.000", "0.000", "0.00"))
		);
	}

	@Test
	void holdsPriceWhenAmountDifferenceExceedsFixedTolerance() {
		assertEquals(
				MatchingStatus.HOLD_PRICE,
				matchingPolicy.decide(totals("10.000", "10.000", "10010000.01"))
		);
	}

	@Test
	void appliesPercentageToleranceWhenItIsSmallerThanFixedTolerance() {
		assertEquals(
				MatchingStatus.MATCHED,
				matchingPolicy.decide(totals("10.000", "1.000", "1.000", "100000.00", "101000.00"))
		);
		assertEquals(
				MatchingStatus.HOLD_PRICE,
				matchingPolicy.decide(totals("10.000", "1.000", "1.000", "100000.00", "101000.01"))
		);
	}

	@Test
	void matchesPartialInvoiceAgainstProratedAmount() {
		assertEquals(
				MatchingStatus.MATCHED,
				matchingPolicy.decide(totals("10.000", "6.000", "6.000", "1000.00", "6000.00"))
		);
	}

	@Test
	void matchesWhenInvoicedQuantityIsLessThanReceivedQuantity() {
		assertEquals(
				MatchingStatus.MATCHED,
				matchingPolicy.decide(totals("10.000", "10.000", "6.000", "1000.00", "6000.00"))
		);
	}

	@Test
	void holdsPriceWhenPartialInvoiceUnitPriceDiffersFromOrder() {
		assertEquals(
				MatchingStatus.HOLD_PRICE,
				matchingPolicy.decide(totals("10.000", "6.000", "6.000", "1000.00", "6600.00"))
		);
	}

	private MatchingLineTotals totals(String receivedQuantity, String invoicedQuantity, String invoicedAmount) {
		return totals("10.000", receivedQuantity, invoicedQuantity, "1000000.00", invoicedAmount);
	}

	private MatchingLineTotals totals(
			String orderedQuantity,
			String receivedQuantity,
			String invoicedQuantity,
			String unitPrice,
			String invoicedAmount
	) {
		BigDecimal unitPriceValue = new BigDecimal(unitPrice);
		return new MatchingLineTotals(
				1L,
				new BigDecimal(orderedQuantity),
				new BigDecimal(receivedQuantity),
				new BigDecimal(invoicedQuantity),
				unitPriceValue,
				unitPriceValue.multiply(new BigDecimal(orderedQuantity)),
				new BigDecimal(invoicedAmount)
		);
	}
}
