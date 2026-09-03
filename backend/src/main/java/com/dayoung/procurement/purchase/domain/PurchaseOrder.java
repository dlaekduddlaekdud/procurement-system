package com.dayoung.procurement.purchase.domain;

import com.dayoung.procurement.masterdata.domain.Item;
import com.dayoung.procurement.masterdata.domain.Vendor;
import com.dayoung.procurement.masterdata.domain.Warehouse;
import com.dayoung.procurement.purchase.exception.InvalidPurchaseOrderStateException;
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
@Table(name = "purchase_order")
public class PurchaseOrder {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "order_number", nullable = false, unique = true, length = 30)
	private String orderNumber;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "purchase_request_id", nullable = false, unique = true)
	private PurchaseRequest purchaseRequest;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "vendor_id", nullable = false)
	private Vendor vendor;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "buyer_id", nullable = false)
	private AppUser buyer;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "warehouse_id", nullable = false)
	private Warehouse warehouse;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private PurchaseOrderStatus status = PurchaseOrderStatus.CREATED;

	@Column(name = "order_date", nullable = false)
	private LocalDate orderDate;

	@Column(name = "expected_delivery_date")
	private LocalDate expectedDeliveryDate;

	@Column(nullable = false, length = 3, columnDefinition = "CHAR(3)")
	private String currency = "KRW";

	@Version
	@Column(nullable = false)
	private long version;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
	private LocalDateTime updatedAt;

	@OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<PurchaseOrderLine> lines = new ArrayList<>();

	protected PurchaseOrder() {
	}

	public PurchaseOrder(
			String orderNumber,
			PurchaseRequest purchaseRequest,
			Vendor vendor,
			AppUser buyer,
			Warehouse warehouse,
			LocalDate orderDate,
			LocalDate expectedDeliveryDate
	) {
		this.orderNumber = orderNumber;
		this.purchaseRequest = purchaseRequest;
		this.vendor = vendor;
		this.buyer = buyer;
		this.warehouse = warehouse;
		this.orderDate = orderDate;
		this.expectedDeliveryDate = expectedDeliveryDate;
	}

	public PurchaseOrderLine addLine(
			PurchaseRequestLine purchaseRequestLine,
			Item item,
			BigDecimal quantity,
			String unit,
			BigDecimal unitPrice,
			BigDecimal supplyAmount,
			BigDecimal taxAmount,
			BigDecimal totalAmount,
			LocalDate expectedDeliveryDate
	) {
		PurchaseOrderLine line = new PurchaseOrderLine(
				this,
				purchaseRequestLine,
				lines.size() + 1,
				item,
				quantity,
				unit,
				unitPrice,
				supplyAmount,
				taxAmount,
				totalAmount,
				expectedDeliveryDate
		);
		lines.add(line);
		return line;
	}

	public void send() {
		if (status != PurchaseOrderStatus.CREATED) {
			throw new InvalidPurchaseOrderStateException(status, "발송");
		}
		this.status = PurchaseOrderStatus.SENT;
	}

	public void applyReceiptStatus(boolean fullyReceived) {
		requireReceivable();
		this.status = fullyReceived ? PurchaseOrderStatus.RECEIVED : PurchaseOrderStatus.PARTIALLY_RECEIVED;
	}

	public void recalculateReceiptStatus(boolean hasReceipt, boolean fullyReceived) {
		if (status != PurchaseOrderStatus.PARTIALLY_RECEIVED && status != PurchaseOrderStatus.RECEIVED) {
			throw new InvalidPurchaseOrderStateException(status, "입고 상태를 재계산");
		}
		if (!hasReceipt) {
			status = PurchaseOrderStatus.SENT;
			return;
		}
		status = fullyReceived ? PurchaseOrderStatus.RECEIVED : PurchaseOrderStatus.PARTIALLY_RECEIVED;
	}

	public void requireReceivable() {
		if (status != PurchaseOrderStatus.SENT && status != PurchaseOrderStatus.PARTIALLY_RECEIVED) {
			throw new InvalidPurchaseOrderStateException(status, "입고");
		}
	}

	public void requireInvoiceAllowed() {
		if (status == PurchaseOrderStatus.CREATED || status == PurchaseOrderStatus.CANCELLED) {
			throw new InvalidPurchaseOrderStateException(status, "송장을 등록");
		}
	}

	public Long getId() {
		return id;
	}

	public String getOrderNumber() {
		return orderNumber;
	}

	public PurchaseRequest getPurchaseRequest() {
		return purchaseRequest;
	}

	public Vendor getVendor() {
		return vendor;
	}

	public AppUser getBuyer() {
		return buyer;
	}

	public Warehouse getWarehouse() {
		return warehouse;
	}

	public PurchaseOrderStatus getStatus() {
		return status;
	}

	public LocalDate getOrderDate() {
		return orderDate;
	}

	public LocalDate getExpectedDeliveryDate() {
		return expectedDeliveryDate;
	}

	public String getCurrency() {
		return currency;
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

	public List<PurchaseOrderLine> getLines() {
		return Collections.unmodifiableList(lines);
	}
}
