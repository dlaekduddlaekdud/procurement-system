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
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "purchase_order_line")
public class PurchaseOrderLine {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "purchase_order_id", nullable = false)
	private PurchaseOrder purchaseOrder;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "purchase_request_line_id", nullable = false, unique = true)
	private PurchaseRequestLine purchaseRequestLine;

	@Column(name = "line_number", nullable = false)
	private int lineNumber;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "item_id", nullable = false)
	private Item item;

	@Column(nullable = false, precision = 19, scale = 3)
	private BigDecimal quantity;

	@Column(nullable = false, length = 20)
	private String unit;

	@Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
	private BigDecimal unitPrice;

	@Column(name = "supply_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal supplyAmount;

	@Column(name = "tax_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal taxAmount;

	@Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal totalAmount;

	@Column(name = "expected_delivery_date")
	private LocalDate expectedDeliveryDate;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
	private LocalDateTime updatedAt;

	protected PurchaseOrderLine() {
	}

	PurchaseOrderLine(
			PurchaseOrder purchaseOrder,
			PurchaseRequestLine purchaseRequestLine,
			int lineNumber,
			Item item,
			BigDecimal quantity,
			String unit,
			BigDecimal unitPrice,
			BigDecimal supplyAmount,
			BigDecimal taxAmount,
			BigDecimal totalAmount,
			LocalDate expectedDeliveryDate
	) {
		this.purchaseOrder = purchaseOrder;
		this.purchaseRequestLine = purchaseRequestLine;
		this.lineNumber = lineNumber;
		this.item = item;
		this.quantity = quantity;
		this.unit = unit;
		this.unitPrice = unitPrice;
		this.supplyAmount = supplyAmount;
		this.taxAmount = taxAmount;
		this.totalAmount = totalAmount;
		this.expectedDeliveryDate = expectedDeliveryDate;
	}

	public Long getId() {
		return id;
	}

	public PurchaseOrder getPurchaseOrder() {
		return purchaseOrder;
	}

	public PurchaseRequestLine getPurchaseRequestLine() {
		return purchaseRequestLine;
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

	public BigDecimal getUnitPrice() {
		return unitPrice;
	}

	public BigDecimal getSupplyAmount() {
		return supplyAmount;
	}

	public BigDecimal getTaxAmount() {
		return taxAmount;
	}

	public BigDecimal getTotalAmount() {
		return totalAmount;
	}

	public LocalDate getExpectedDeliveryDate() {
		return expectedDeliveryDate;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}
}
