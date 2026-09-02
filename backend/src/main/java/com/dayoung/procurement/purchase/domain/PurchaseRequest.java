package com.dayoung.procurement.purchase.domain;

import com.dayoung.procurement.masterdata.domain.Department;
import com.dayoung.procurement.masterdata.domain.Item;
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
		PurchaseRequestLine line = new PurchaseRequestLine(
				this,
				lines.size() + 1,
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
