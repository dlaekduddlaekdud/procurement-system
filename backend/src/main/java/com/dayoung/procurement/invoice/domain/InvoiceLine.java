package com.dayoung.procurement.invoice.domain;

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
@Table(name = "invoice_line")
public class InvoiceLine {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "invoice_id", nullable = false)
	private Invoice invoice;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "purchase_order_line_id", nullable = false)
	private PurchaseOrderLine purchaseOrderLine;

	@Column(name = "line_number", nullable = false)
	private int lineNumber;

	@Column(nullable = false, precision = 19, scale = 3)
	private BigDecimal quantity;

	@Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
	private BigDecimal unitPrice;

	@Column(name = "supply_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal supplyAmount;

	@Column(name = "tax_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal taxAmount;

	@Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal totalAmount;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
	private LocalDateTime updatedAt;

	protected InvoiceLine() {
	}

	InvoiceLine(
			Invoice invoice,
			PurchaseOrderLine purchaseOrderLine,
			int lineNumber,
			BigDecimal quantity,
			BigDecimal unitPrice,
			BigDecimal supplyAmount,
			BigDecimal taxAmount,
			BigDecimal totalAmount
	) {
		this.invoice = invoice;
		this.purchaseOrderLine = purchaseOrderLine;
		this.lineNumber = lineNumber;
		this.quantity = quantity;
		this.unitPrice = unitPrice;
		this.supplyAmount = supplyAmount;
		this.taxAmount = taxAmount;
		this.totalAmount = totalAmount;
	}

	public Long getId() {
		return id;
	}

	public Invoice getInvoice() {
		return invoice;
	}

	public PurchaseOrderLine getPurchaseOrderLine() {
		return purchaseOrderLine;
	}

	public int getLineNumber() {
		return lineNumber;
	}

	public BigDecimal getQuantity() {
		return quantity;
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

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}
}
