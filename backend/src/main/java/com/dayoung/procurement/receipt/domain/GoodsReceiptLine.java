package com.dayoung.procurement.receipt.domain;

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
@Table(name = "goods_receipt_line")
public class GoodsReceiptLine {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "goods_receipt_id", nullable = false)
	private GoodsReceipt goodsReceipt;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "purchase_order_line_id", nullable = false)
	private PurchaseOrderLine purchaseOrderLine;

	@Column(name = "line_number", nullable = false)
	private int lineNumber;

	@Column(nullable = false, precision = 19, scale = 3)
	private BigDecimal quantity;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
	private LocalDateTime updatedAt;

	protected GoodsReceiptLine() {
	}

	GoodsReceiptLine(
			GoodsReceipt goodsReceipt,
			PurchaseOrderLine purchaseOrderLine,
			int lineNumber,
			BigDecimal quantity
	) {
		this.goodsReceipt = goodsReceipt;
		this.purchaseOrderLine = purchaseOrderLine;
		this.lineNumber = lineNumber;
		this.quantity = quantity;
	}

	public Long getId() {
		return id;
	}

	public GoodsReceipt getGoodsReceipt() {
		return goodsReceipt;
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

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}
}
