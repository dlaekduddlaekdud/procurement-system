package com.dayoung.procurement.matching.domain;

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
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "match_result")
public class MatchResult {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "purchase_order_line_id", nullable = false, unique = true)
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

	@Version
	@Column(nullable = false)
	private long version;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
	private LocalDateTime updatedAt;

	protected MatchResult() {
	}

	public MatchResult(PurchaseOrderLine purchaseOrderLine) {
		this.purchaseOrderLine = purchaseOrderLine;
	}

	public void update(
			MatchingStatus status,
			BigDecimal orderedQuantity,
			BigDecimal receivedQuantity,
			BigDecimal invoicedQuantity,
			BigDecimal orderedAmount,
			BigDecimal invoicedAmount
	) {
		this.status = status;
		this.orderedQuantity = orderedQuantity;
		this.receivedQuantity = receivedQuantity;
		this.invoicedQuantity = invoicedQuantity;
		this.orderedAmount = orderedAmount;
		this.invoicedAmount = invoicedAmount;
	}

	public Long getId() {
		return id;
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

	public long getVersion() {
		return version;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}
}
