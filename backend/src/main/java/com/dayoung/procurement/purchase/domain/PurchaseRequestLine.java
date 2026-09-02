package com.dayoung.procurement.purchase.domain;

import com.dayoung.procurement.masterdata.domain.Item;
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
@Table(name = "purchase_request_line")
public class PurchaseRequestLine {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "purchase_request_id", nullable = false)
	private PurchaseRequest purchaseRequest;

	@Column(name = "line_number", nullable = false)
	private int lineNumber;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "item_id", nullable = false)
	private Item item;

	@Column(nullable = false, precision = 19, scale = 3)
	private BigDecimal quantity;

	@Column(nullable = false, length = 20)
	private String unit;

	@Column(name = "estimated_unit_price", nullable = false, precision = 19, scale = 2)
	private BigDecimal estimatedUnitPrice;

	@Column(name = "estimated_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal estimatedAmount;

	@Column(length = 500)
	private String description;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
	private LocalDateTime updatedAt;

	protected PurchaseRequestLine() {
	}

	PurchaseRequestLine(
			PurchaseRequest purchaseRequest,
			int lineNumber,
			Item item,
			BigDecimal quantity,
			String unit,
			BigDecimal estimatedUnitPrice,
			BigDecimal estimatedAmount,
			String description
	) {
		this.purchaseRequest = purchaseRequest;
		this.lineNumber = lineNumber;
		this.item = item;
		this.quantity = quantity;
		this.unit = unit;
		this.estimatedUnitPrice = estimatedUnitPrice;
		this.estimatedAmount = estimatedAmount;
		this.description = description;
	}

	public Long getId() {
		return id;
	}

	public PurchaseRequest getPurchaseRequest() {
		return purchaseRequest;
	}

	public int getLineNumber() {
		return lineNumber;
	}

	public Item getItem() {
		return item;
	}

	public BigDecimal getQuantity() {
		return quantity;
	}

	public String getUnit() {
		return unit;
	}

	public BigDecimal getEstimatedUnitPrice() {
		return estimatedUnitPrice;
	}

	public BigDecimal getEstimatedAmount() {
		return estimatedAmount;
	}

	public String getDescription() {
		return description;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}
}
