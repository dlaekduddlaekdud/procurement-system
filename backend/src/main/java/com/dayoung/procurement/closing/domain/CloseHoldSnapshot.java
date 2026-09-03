package com.dayoung.procurement.closing.domain;

import com.dayoung.procurement.matching.domain.MatchResult;
import com.dayoung.procurement.matching.domain.MatchingStatus;
import com.dayoung.procurement.purchase.domain.PurchaseOrderLine;
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
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "close_hold_snapshot")
public class CloseHoldSnapshot {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "close_run_id", nullable = false)
	private CloseRun closeRun;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "match_result_id", nullable = false)
	private MatchResult matchResult;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "purchase_order_line_id", nullable = false)
	private PurchaseOrderLine purchaseOrderLine;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private MatchingStatus status;

	@Column(name = "ordered_quantity", nullable = false, precision = 19, scale = 3)
	private BigDecimal orderedQuantity;

	@Column(name = "received_quantity", nullable = false, precision = 19, scale = 3)
	private BigDecimal receivedQuantity;

	@Column(name = "invoiced_quantity", nullable = false, precision = 19, scale = 3)
	private BigDecimal invoicedQuantity;

	@Column(name = "ordered_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal orderedAmount;

	@Column(name = "invoiced_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal invoicedAmount;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private LocalDateTime createdAt;

	protected CloseHoldSnapshot() {
	}

	public CloseHoldSnapshot(CloseRun closeRun, MatchResult matchResult) {
		if (closeRun == null || matchResult == null) {
			throw new IllegalArgumentException("마감 실행과 대사 결과는 필수입니다.");
		}
		if (matchResult.getStatus() != MatchingStatus.HOLD_QUANTITY
				&& matchResult.getStatus() != MatchingStatus.HOLD_PRICE) {
			throw new IllegalArgumentException("HOLD 상태의 대사 결과만 스냅샷으로 저장할 수 있습니다.");
		}
		this.closeRun = closeRun;
		this.matchResult = matchResult;
		this.purchaseOrderLine = matchResult.getPurchaseOrderLine();
		this.status = matchResult.getStatus();
		this.orderedQuantity = matchResult.getOrderedQuantity();
		this.receivedQuantity = matchResult.getReceivedQuantity();
		this.invoicedQuantity = matchResult.getInvoicedQuantity();
		this.orderedAmount = matchResult.getOrderedAmount();
		this.invoicedAmount = matchResult.getInvoicedAmount();
	}

	public Long getId() {
		return id;
	}

	public CloseRun getCloseRun() {
		return closeRun;
	}

	public MatchResult getMatchResult() {
		return matchResult;
	}

	public PurchaseOrderLine getPurchaseOrderLine() {
		return purchaseOrderLine;
	}

	public MatchingStatus getStatus() {
		return status;
	}

	public BigDecimal getOrderedQuantity() {
		return orderedQuantity;
	}

	public BigDecimal getReceivedQuantity() {
		return receivedQuantity;
	}

	public BigDecimal getInvoicedQuantity() {
		return invoicedQuantity;
	}

	public BigDecimal getOrderedAmount() {
		return orderedAmount;
	}

	public BigDecimal getInvoicedAmount() {
		return invoicedAmount;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}
