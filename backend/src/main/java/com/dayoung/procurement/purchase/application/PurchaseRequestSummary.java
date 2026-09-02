package com.dayoung.procurement.purchase.application;

import com.dayoung.procurement.purchase.domain.PurchaseRequestStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PurchaseRequestSummary(
		Long id,
		String requestNumber,
		String title,
		PurchaseRequestStatus status,
		LocalDate requestDate,
		LocalDate neededDate,
		BigDecimal totalEstimatedAmount,
		LocalDateTime createdAt
) {
}
