package com.dayoung.procurement.ledger.domain;

import com.dayoung.procurement.purchase.domain.PurchaseOrderLine;
import com.dayoung.procurement.receipt.domain.GoodsReceiptLine;
import com.dayoung.procurement.user.domain.AppUser;
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
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "accrual_entry")
public class AccrualEntry {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "entry_number", nullable = false, unique = true, length = 40)
	private String entryNumber;

	@Enumerated(EnumType.STRING)
	@Column(name = "entry_type", nullable = false, length = 30)
	private AccrualEntryType entryType;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "purchase_order_line_id", nullable = false)
	private PurchaseOrderLine purchaseOrderLine;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "goods_receipt_line_id")
	private GoodsReceiptLine goodsReceiptLine;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal amount;

	@Column(nullable = false, length = 3, columnDefinition = "CHAR(3)")
	private String currency;

	@Column(name = "posting_date", nullable = false)
	private LocalDate postingDate;

	@Column(nullable = false, length = 6, columnDefinition = "CHAR(6)")
	private String period;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "created_by", nullable = false)
	private AppUser createdBy;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "reversal_of_id")
	private AccrualEntry reversalOf;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private LocalDateTime createdAt;

	protected AccrualEntry() {
	}

	public AccrualEntry(
			String entryNumber,
			PurchaseOrderLine purchaseOrderLine,
			GoodsReceiptLine goodsReceiptLine,
			BigDecimal amount,
			String currency,
			LocalDate postingDate,
			String period,
			AppUser createdBy
	) {
		this.entryNumber = entryNumber;
		this.entryType = AccrualEntryType.GR_ACCRUAL;
		this.purchaseOrderLine = purchaseOrderLine;
		this.goodsReceiptLine = goodsReceiptLine;
		this.amount = amount;
		this.currency = currency;
		this.postingDate = postingDate;
		this.period = period;
		this.createdBy = createdBy;
	}

	public static AccrualEntry forInvoiceMatch(
			String entryNumber,
			PurchaseOrderLine purchaseOrderLine,
			BigDecimal amount,
			String currency,
			LocalDate postingDate,
			String period,
			AppUser createdBy
	) {
		AccrualEntry entry = new AccrualEntry();
		entry.entryNumber = entryNumber;
		entry.entryType = AccrualEntryType.INVOICE_MATCH;
		entry.purchaseOrderLine = purchaseOrderLine;
		entry.amount = amount;
		entry.currency = currency;
		entry.postingDate = postingDate;
		entry.period = period;
		entry.createdBy = createdBy;
		return entry;
	}

	public Long getId() {
		return id;
	}

	public String getEntryNumber() {
		return entryNumber;
	}

	public AccrualEntryType getEntryType() {
		return entryType;
	}

	public PurchaseOrderLine getPurchaseOrderLine() {
		return purchaseOrderLine;
	}

	public GoodsReceiptLine getGoodsReceiptLine() {
		return goodsReceiptLine;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public String getCurrency() {
		return currency;
	}

	public LocalDate getPostingDate() {
		return postingDate;
	}

	public String getPeriod() {
		return period;
	}

	public AppUser getCreatedBy() {
		return createdBy;
	}

	public AccrualEntry getReversalOf() {
		return reversalOf;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}
