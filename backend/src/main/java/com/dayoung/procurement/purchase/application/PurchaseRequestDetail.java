package com.dayoung.procurement.purchase.application;

import com.dayoung.procurement.purchase.domain.PurchaseRequestStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PurchaseRequestDetail(
		Long id,
		String requestNumber,
		Long requesterId,
		String requesterName,
		Long departmentId,
		String departmentCode,
		String departmentName,
		String title,
		String purpose,
		PurchaseRequestStatus status,
		LocalDate requestDate,
		LocalDate neededDate,
		LocalDateTime submittedAt,
		LocalDateTime approvedAt,
		String rejectionReason,
		LocalDateTime rejectedAt,
		BigDecimal totalEstimatedAmount,
		List<PurchaseRequestLineDetail> lines,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
}
