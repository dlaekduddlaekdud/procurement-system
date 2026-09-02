package com.dayoung.procurement.matching.application;

import com.dayoung.procurement.matching.domain.MatchingStatus;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class ThreeWayMatchingPolicy {

	private static final BigDecimal AMOUNT_TOLERANCE_RATE = new BigDecimal("0.01");
	private static final BigDecimal MAX_AMOUNT_TOLERANCE = new BigDecimal("10000.00");

	public MatchingStatus decide(MatchingLineTotals totals) {
		if (totals.receivedQuantity().compareTo(totals.invoicedQuantity()) != 0) {
			return MatchingStatus.HOLD_QUANTITY;
		}

		BigDecimal amountDifference = totals.invoicedAmount()
				.subtract(totals.orderedAmount())
				.abs();
		BigDecimal amountTolerance = totals.orderedAmount()
				.abs()
				.multiply(AMOUNT_TOLERANCE_RATE)
				.min(MAX_AMOUNT_TOLERANCE);

		if (amountDifference.compareTo(amountTolerance) > 0) {
			return MatchingStatus.HOLD_PRICE;
		}
		return MatchingStatus.MATCHED;
	}
}
