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
	void matchesWhenQuantityIsEqualAndAmountDifferenceIsWithinTolerance() {
		assertEquals(MatchingStatus.MATCHED, matchingPolicy.decide(totals("10.000", "10.000", "10000000.00")));
		assertEquals(MatchingStatus.MATCHED, matchingPolicy.decide(totals("10.000", "10.000", "10005000.00")));
		assertEquals(MatchingStatus.MATCHED, matchingPolicy.decide(totals("10.000", "10.000", "10010000.00")));
	}

	@Test
	void holdsQuantityBeforeEvaluatingAmountDifference() {
		assertEquals(
				MatchingStatus.HOLD_QUANTITY,
				matchingPolicy.decide(totals("8.000", "10.000", "10050000.00"))
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
		assertEquals(MatchingStatus.MATCHED, matchingPolicy.decide(totals("1.000", "1.000", "101000.00", "100000.00")));
		assertEquals(MatchingStatus.HOLD_PRICE, matchingPolicy.decide(totals("1.000", "1.000", "101000.01", "100000.00")));
	}

	private MatchingLineTotals totals(String receivedQuantity, String invoicedQuantity, String invoicedAmount) {
		return totals(receivedQuantity, invoicedQuantity, invoicedAmount, "10000000.00");
	}

	private MatchingLineTotals totals(
			String receivedQuantity,
			String invoicedQuantity,
			String invoicedAmount,
			String orderedAmount
	) {
		return new MatchingLineTotals(
				1L,
				new BigDecimal("10.000"),
				new BigDecimal(receivedQuantity),
				new BigDecimal(invoicedQuantity),
				new BigDecimal(orderedAmount),
				new BigDecimal(invoicedAmount)
		);
	}
}
