package com.dayoung.procurement.matching.application;

import com.dayoung.procurement.matching.domain.MatchingStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class ThreeWayMatchingPolicy {

	private static final BigDecimal AMOUNT_TOLERANCE_RATE = new BigDecimal("0.01");
	private static final BigDecimal MAX_AMOUNT_TOLERANCE = new BigDecimal("10000.00");

	public MatchingStatus decide(MatchingLineTotals totals) {
		// 접수된 송장이 없으면 대사 대상이 아니므로 MATCHED로 판정하지 않는다.
		if (totals.invoicedQuantity().signum() <= 0) {
			return MatchingStatus.HOLD_QUANTITY;
		}

		// 발주수량 초과는 입고 등록에서 이미 차단되므로, 대사에서는 입고분을 넘는 송장만 차단한다.
		if (totals.invoicedQuantity().compareTo(totals.receivedQuantity()) > 0) {
			return MatchingStatus.HOLD_QUANTITY;
		}

		// 부분 송장도 대사할 수 있도록 발주 라인 전액이 아닌 송장 수량 기준 안분액과 비교한다.
		BigDecimal expectedAmount = expectedAmount(totals);
		BigDecimal amountDifference = totals.invoicedAmount()
				.subtract(expectedAmount)
				.abs();
		BigDecimal amountTolerance = expectedAmount
				.abs()
				.multiply(AMOUNT_TOLERANCE_RATE)
				.min(MAX_AMOUNT_TOLERANCE);

		if (amountDifference.compareTo(amountTolerance) > 0) {
			return MatchingStatus.HOLD_PRICE;
		}
		return MatchingStatus.MATCHED;
	}

	// 송장 라인 금액과 동일한 반올림 규칙을 적용해야 1원 단위 차이로 HOLD가 발생하지 않는다.
	private BigDecimal expectedAmount(MatchingLineTotals totals) {
		return totals.unitPrice()
				.multiply(totals.invoicedQuantity())
				.setScale(2, RoundingMode.HALF_UP);
	}
}
