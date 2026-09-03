package com.dayoung.procurement.receipt.domain;

import com.dayoung.procurement.masterdata.domain.Warehouse;
import com.dayoung.procurement.purchase.domain.PurchaseOrder;
import com.dayoung.procurement.purchase.domain.PurchaseOrderLine;
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
@Table(name = "goods_receipt")
public class GoodsReceipt {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "receipt_number", nullable = false, unique = true, length = 30)
	private String receiptNumber;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "purchase_order_id", nullable = false)
	private PurchaseOrder purchaseOrder;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "warehouse_id", nullable = false)
	private Warehouse warehouse;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "received_by", nullable = false)
	private AppUser receivedBy;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private GoodsReceiptStatus status = GoodsReceiptStatus.POSTED;

	@Column(name = "posting_date", nullable = false)
	private LocalDate postingDate;

	@Column(name = "received_at", nullable = false)
	private LocalDateTime receivedAt;

	@Version
	@Column(nullable = false)
	private long version;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
	private LocalDateTime updatedAt;

	@OneToMany(mappedBy = "goodsReceipt", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<GoodsReceiptLine> lines = new ArrayList<>();

	protected GoodsReceipt() {
	}

	public GoodsReceipt(
			String receiptNumber,
			PurchaseOrder purchaseOrder,
			Warehouse warehouse,
			AppUser receivedBy,
			LocalDate postingDate,
			LocalDateTime receivedAt
	) {
		this.receiptNumber = receiptNumber;
		this.purchaseOrder = purchaseOrder;
		this.warehouse = warehouse;
		this.receivedBy = receivedBy;
		this.postingDate = postingDate;
		this.receivedAt = receivedAt;
	}

	public GoodsReceiptLine addLine(PurchaseOrderLine purchaseOrderLine, BigDecimal quantity) {
		GoodsReceiptLine line = new GoodsReceiptLine(this, purchaseOrderLine, lines.size() + 1, quantity);
		lines.add(line);
		return line;
	}

	public Long getId() {
		return id;
	}

	public String getReceiptNumber() {
		return receiptNumber;
	}

	public PurchaseOrder getPurchaseOrder() {
		return purchaseOrder;
	}

	public Warehouse getWarehouse() {
		return warehouse;
	}

	public AppUser getReceivedBy() {
		return receivedBy;
	}

	public GoodsReceiptStatus getStatus() {
		return status;
	}

	public LocalDate getPostingDate() {
		return postingDate;
	}

	public LocalDateTime getReceivedAt() {
		return receivedAt;
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

	public List<GoodsReceiptLine> getLines() {
		return Collections.unmodifiableList(lines);
	}
}
