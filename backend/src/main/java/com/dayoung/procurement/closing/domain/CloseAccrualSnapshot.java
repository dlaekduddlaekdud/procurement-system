package com.dayoung.procurement.closing.domain;

import com.dayoung.procurement.purchase.domain.PurchaseOrderLine;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "close_accrual_snapshot")
public class CloseAccrualSnapshot {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "close_run_id", nullable = false)
	private CloseRun closeRun;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "purchase_order_line_id", nullable = false)
	private PurchaseOrderLine purchaseOrderLine;

	@Column(name = "balance_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal balanceAmount;

	@Column(nullable = false, length = 3, columnDefinition = "CHAR(3)")
	private String currency;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private LocalDateTime createdAt;

	protected CloseAccrualSnapshot() {
	}

	public CloseAccrualSnapshot(
			CloseRun closeRun,
			PurchaseOrderLine purchaseOrderLine,
			BigDecimal balanceAmount,
			String currency
	) {
		if (closeRun == null || purchaseOrderLine == null) {
			throw new IllegalArgumentException("마감 실행과 발주 라인은 필수입니다.");
		}
		if (balanceAmount == null || balanceAmount.signum() == 0) {
			throw new IllegalArgumentException("미착 잔액은 0이 아니어야 합니다.");
		}
		if (currency == null || currency.length() != 3) {
			throw new IllegalArgumentException("통화 코드는 3자리여야 합니다.");
		}
		this.closeRun = closeRun;
		this.purchaseOrderLine = purchaseOrderLine;
		this.balanceAmount = balanceAmount;
		this.currency = currency;
	}

	public Long getId() {
		return id;
	}

	public CloseRun getCloseRun() {
		return closeRun;
	}

	public PurchaseOrderLine getPurchaseOrderLine() {
		return purchaseOrderLine;
	}

	public BigDecimal getBalanceAmount() {
		return balanceAmount;
	}

	public String getCurrency() {
		return currency;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}
