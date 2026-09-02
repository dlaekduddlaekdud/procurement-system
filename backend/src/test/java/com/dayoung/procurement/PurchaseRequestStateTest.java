package com.dayoung.procurement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dayoung.procurement.masterdata.domain.Department;
import com.dayoung.procurement.masterdata.domain.Item;
import com.dayoung.procurement.purchase.domain.PurchaseRequest;
import com.dayoung.procurement.purchase.domain.PurchaseRequestStatus;
import com.dayoung.procurement.purchase.exception.InvalidPurchaseRequestStateException;
import com.dayoung.procurement.purchase.exception.SelfApprovalNotAllowedException;
import com.dayoung.procurement.user.domain.AppUser;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PurchaseRequestStateTest {

	private AppUser requester;
	private AppUser buyer;
	private PurchaseRequest request;

	@BeforeEach
	void setUp() {
		Department department = new Department("TEST-IT", "테스트 IT팀", null);
		requester = new AppUser("requester-state@example.com", "encoded-password", "요청자", department);
		buyer = new AppUser("buyer-state@example.com", "encoded-password", "구매 담당자", department);
		request = new PurchaseRequest(
				"PR-STATE-TEST",
				requester,
				department,
				"테스트 구매요청",
				"상태 전이 검증",
				LocalDate.of(2026, 9, 2),
				LocalDate.of(2026, 9, 15)
		);
	}

	@Test
	void submitsDraftRequestWithLine() {
		addLine();
		LocalDateTime submittedAt = LocalDateTime.of(2026, 9, 2, 10, 0);

		request.submit(submittedAt);

		assertEquals(PurchaseRequestStatus.SUBMITTED, request.getStatus());
		assertEquals(submittedAt, request.getSubmittedAt());
	}

	@Test
	void rejectsSubmissionWithoutLine() {
		assertThrows(InvalidPurchaseRequestStateException.class,
				() -> request.submit(LocalDateTime.of(2026, 9, 2, 10, 0)));
	}

	@Test
	void rejectsAddingLineAfterSubmission() {
		addLine();
		request.submit(LocalDateTime.of(2026, 9, 2, 10, 0));

		assertThrows(InvalidPurchaseRequestStateException.class, this::addLine);
	}

	@Test
	void rejectsRemovingLineAfterSubmission() {
		addLine();
		request.submit(LocalDateTime.of(2026, 9, 2, 10, 0));

		assertThrows(InvalidPurchaseRequestStateException.class, () -> request.removeLine(1L));
	}

	@Test
	void approvesSubmittedRequest() {
		addLine();
		request.submit(LocalDateTime.of(2026, 9, 2, 10, 0));
		LocalDateTime approvedAt = LocalDateTime.of(2026, 9, 2, 11, 0);

		request.approve(buyer, approvedAt);

		assertEquals(PurchaseRequestStatus.APPROVED, request.getStatus());
		assertEquals(buyer, request.getApprovedBy());
		assertEquals(approvedAt, request.getApprovedAt());
	}

	@Test
	void rejectsSelfApproval() {
		addLine();
		request.submit(LocalDateTime.of(2026, 9, 2, 10, 0));

		assertThrows(SelfApprovalNotAllowedException.class,
				() -> request.approve(requester, LocalDateTime.of(2026, 9, 2, 11, 0)));
	}

	@Test
	void rejectsSubmittedRequestWithReason() {
		addLine();
		request.submit(LocalDateTime.of(2026, 9, 2, 10, 0));
		LocalDateTime rejectedAt = LocalDateTime.of(2026, 9, 2, 11, 0);

		request.reject(buyer, rejectedAt, "예산을 초과했습니다.");

		assertEquals(PurchaseRequestStatus.REJECTED, request.getStatus());
		assertEquals(buyer, request.getRejectedBy());
		assertEquals(rejectedAt, request.getRejectedAt());
		assertEquals("예산을 초과했습니다.", request.getRejectionReason());
	}

	private void addLine() {
		Item item = new Item(
				"ITEM-STATE-TEST",
				"테스트 품목",
				null,
				"EA",
				new BigDecimal("1000.00")
		);
		request.addLine(
				item,
				new BigDecimal("2.000"),
				"EA",
				new BigDecimal("1000.00"),
				new BigDecimal("2000.00"),
				null
		);
	}
}
