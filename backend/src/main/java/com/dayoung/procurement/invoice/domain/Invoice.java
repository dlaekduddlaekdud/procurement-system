package com.dayoung.procurement.invoice.domain;

import com.dayoung.procurement.masterdata.domain.Vendor;
import com.dayoung.procurement.invoice.exception.InvalidInvoiceStateException;
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
@Table(name = "invoice")
public class Invoice {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "invoice_number", nullable = false, length = 50)
	private String invoiceNumber;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "purchase_order_id", nullable = false)
	private PurchaseOrder purchaseOrder;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "vendor_id", nullable = false)
	private Vendor vendor;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "received_by", nullable = false)
	private AppUser receivedBy;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private InvoiceStatus status = InvoiceStatus.RECEIVED;

	@Column(name = "invoice_date", nullable = false)
	private LocalDate invoiceDate;

	@Column(name = "posting_date", nullable = false)
	private LocalDate postingDate;

	@Column(nullable = false, length = 3, columnDefinition = "CHAR(3)")
	private String currency;

	@Column(name = "supply_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal supplyAmount = BigDecimal.ZERO.setScale(2);

	@Column(name = "tax_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal taxAmount = BigDecimal.ZERO.setScale(2);

	@Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal totalAmount = BigDecimal.ZERO.setScale(2);

	@Version
	@Column(nullable = false)
	private long version;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
	private LocalDateTime updatedAt;

	@OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<InvoiceLine> lines = new ArrayList<>();

	protected Invoice() {
	}

	public Invoice(
			String invoiceNumber,
			PurchaseOrder purchaseOrder,
			Vendor vendor,
			AppUser receivedBy,
			LocalDate invoiceDate,
			LocalDate postingDate,
			String currency
	) {
		this.invoiceNumber = invoiceNumber;
		this.purchaseOrder = purchaseOrder;
		this.vendor = vendor;
		this.receivedBy = receivedBy;
		this.invoiceDate = invoiceDate;
		this.postingDate = postingDate;
		this.currency = currency;
	}

	public InvoiceLine addLine(PurchaseOrderLine purchaseOrderLine, BigDecimal quantity, BigDecimal unitPrice) {
		InvoiceLineAmounts amounts = InvoiceLineAmounts.calculate(quantity, unitPrice);
		InvoiceLine line = new InvoiceLine(
				this,
				purchaseOrderLine,
				lines.size() + 1,
				quantity,
				unitPrice,
				amounts.supplyAmount(),
				amounts.taxAmount(),
				amounts.totalAmount()
		);
		lines.add(line);
		supplyAmount = supplyAmount.add(amounts.supplyAmount());
		taxAmount = taxAmount.add(amounts.taxAmount());
		totalAmount = totalAmount.add(amounts.totalAmount());
		return line;
	}

	public void cancel() {
		if (status != InvoiceStatus.RECEIVED) {
			throw new InvalidInvoiceStateException(status);
		}
		status = InvoiceStatus.CANCELLED;
	}

	public Long getId() {
		return id;
	}

	public String getInvoiceNumber() {
		return invoiceNumber;
	}

	public PurchaseOrder getPurchaseOrder() {
		return purchaseOrder;
	}

	public Vendor getVendor() {
		return vendor;
	}

	public AppUser getReceivedBy() {
		return receivedBy;
	}

	public InvoiceStatus getStatus() {
		return status;
	}

	public LocalDate getInvoiceDate() {
		return invoiceDate;
	}

	public LocalDate getPostingDate() {
		return postingDate;
	}

	public String getCurrency() {
		return currency;
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

	public long getVersion() {
		return version;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public List<InvoiceLine> getLines() {
		return Collections.unmodifiableList(lines);
	}
}
