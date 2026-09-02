package com.dayoung.procurement.purchase.domain;

import com.dayoung.procurement.masterdata.domain.Department;
import com.dayoung.procurement.masterdata.domain.Item;
import com.dayoung.procurement.purchase.exception.InvalidPurchaseRequestStateException;
import com.dayoung.procurement.purchase.exception.PurchaseRequestLineNotFoundException;
import com.dayoung.procurement.purchase.exception.SelfApprovalNotAllowedException;
import com.dayoung.procurement.user.domain.AppUser;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "purchase_request")
public class PurchaseRequest {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "request_number", nullable = false, unique = true, length = 30)
	private String requestNumber;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "requester_id", nullable = false)
	private AppUser requester;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "department_id", nullable = false)
	private Department department;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(length = 1000)
	private String purpose;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private PurchaseRequestStatus status = PurchaseRequestStatus.DRAFT;

	@Column(name = "request_date", nullable = false)
	private LocalDate requestDate;

	@Column(name = "needed_date")
	private LocalDate neededDate;

	@Column(name = "submitted_at")
	private LocalDateTime submittedAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "approved_by")
	private AppUser approvedBy;

	@Column(name = "approved_at")
	private LocalDateTime approvedAt;

	@Column(name = "rejection_reason", length = 500)
	private String rejectionReason;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "rejected_by")
	private AppUser rejectedBy;

	@Column(name = "rejected_at")
	private LocalDateTime rejectedAt;

	@Version
	@Column(nullable = false)
	private long version;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
	private LocalDateTime updatedAt;

	@OneToMany(mappedBy = "purchaseRequest", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<PurchaseRequestLine> lines = new ArrayList<>();

	protected PurchaseRequest() {
	}

	public PurchaseRequest(
			String requestNumber,
			AppUser requester,
			Department department,
			String title,
			String purpose,
			LocalDate requestDate,
			LocalDate neededDate
	) {
		this.requestNumber = requestNumber;
		this.requester = requester;
		this.department = department;
		this.title = title;
		this.purpose = purpose;
		this.requestDate = requestDate;
		this.neededDate = neededDate;
	}

	public PurchaseRequestLine addLine(
			Item item,
			BigDecimal quantity,
			String unit,
			BigDecimal estimatedUnitPrice,
			BigDecimal estimatedAmount,
			String description
	) {
		requireStatus(PurchaseRequestStatus.DRAFT, "품목을 추가");
		int nextLineNumber = lines.stream()
				.mapToInt(PurchaseRequestLine::getLineNumber)
				.max()
				.orElse(0) + 1;
		PurchaseRequestLine line = new PurchaseRequestLine(
				this,
				nextLineNumber,
				item,
				quantity,
				unit,
				estimatedUnitPrice,
				estimatedAmount,
				description
		);
		lines.add(line);
		return line;
	}

	public void removeLine(Long lineId) {
		requireStatus(PurchaseRequestStatus.DRAFT, "품목을 삭제");
		boolean removed = lines.removeIf(
				line -> line.getId() != null && line.getId().equals(lineId)
		);
		if (!removed) {
			throw new PurchaseRequestLineNotFoundException(id, lineId);
		}
	}

	public void updateDetails(String title, String purpose, LocalDate neededDate) {
		requireStatus(PurchaseRequestStatus.DRAFT, "수정");
		this.title = title;
		this.purpose = purpose;
		this.neededDate = neededDate;
	}

	public void requireApprovedForOrder() {
		requireStatus(PurchaseRequestStatus.APPROVED, "발주로 전환");
	}

	public void submit(LocalDateTime submittedAt) {
		requireStatus(PurchaseRequestStatus.DRAFT, "제출");
		if (lines.isEmpty()) {
			throw new InvalidPurchaseRequestStateException("품목이 없는 구매요청은 제출할 수 없습니다.");
		}
		if (submittedAt == null) {
			throw new IllegalArgumentException("제출 시각은 필수입니다.");
		}

		this.status = PurchaseRequestStatus.SUBMITTED;
		this.submittedAt = submittedAt;
	}

	public void approve(AppUser approver, LocalDateTime approvedAt) {
		requireStatus(PurchaseRequestStatus.SUBMITTED, "승인");
		validateDecisionMaker(approver);
		if (approvedAt == null) {
			throw new IllegalArgumentException("승인 시각은 필수입니다.");
		}

		this.status = PurchaseRequestStatus.APPROVED;
		this.approvedBy = approver;
		this.approvedAt = approvedAt;
	}

	public void reject(AppUser rejector, LocalDateTime rejectedAt, String reason) {
		requireStatus(PurchaseRequestStatus.SUBMITTED, "거절");
		validateDecisionMaker(rejector);
		if (rejectedAt == null) {
			throw new IllegalArgumentException("거절 시각은 필수입니다.");
		}
		if (reason == null || reason.isBlank()) {
			throw new IllegalArgumentException("거절 사유는 필수입니다.");
		}

		this.status = PurchaseRequestStatus.REJECTED;
		this.rejectedBy = rejector;
		this.rejectedAt = rejectedAt;
		this.rejectionReason = reason;
	}

	private void requireStatus(PurchaseRequestStatus requiredStatus, String action) {
		if (status != requiredStatus) {
			throw new InvalidPurchaseRequestStateException(status, action);
		}
	}

	private void validateDecisionMaker(AppUser decisionMaker) {
		if (decisionMaker == null) {
			throw new IllegalArgumentException("처리자는 필수입니다.");
		}
		boolean sameInstance = requester == decisionMaker;
		boolean sameId = requester.getId() != null && requester.getId().equals(decisionMaker.getId());
		if (sameInstance || sameId) {
			throw new SelfApprovalNotAllowedException();
		}
	}

	public Long getId() {
		return id;
	}

	public String getRequestNumber() {
		return requestNumber;
	}

	public AppUser getRequester() {
		return requester;
	}

	public Department getDepartment() {
		return department;
	}

	public String getTitle() {
		return title;
	}

	public String getPurpose() {
		return purpose;
	}

	public PurchaseRequestStatus getStatus() {
		return status;
	}

	public LocalDate getRequestDate() {
		return requestDate;
	}

	public LocalDate getNeededDate() {
		return neededDate;
	}

	public LocalDateTime getSubmittedAt() {
		return submittedAt;
	}

	public AppUser getApprovedBy() {
		return approvedBy;
	}

	public LocalDateTime getApprovedAt() {
		return approvedAt;
	}

	public String getRejectionReason() {
		return rejectionReason;
	}

	public AppUser getRejectedBy() {
		return rejectedBy;
	}

	public LocalDateTime getRejectedAt() {
		return rejectedAt;
	}

	public long getVersion() {
		return version;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public List<PurchaseRequestLine> getLines() {
		return Collections.unmodifiableList(lines);
	}
}
