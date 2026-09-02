package com.dayoung.procurement.matching.application;

import com.dayoung.procurement.invoice.domain.InvoiceStatus;
import com.dayoung.procurement.invoice.repository.InvoiceLineRepository;
import com.dayoung.procurement.ledger.application.AccrualEntryService;
import com.dayoung.procurement.matching.domain.MatchResult;
import com.dayoung.procurement.matching.domain.MatchingStatus;
import com.dayoung.procurement.matching.repository.MatchResultRepository;
import com.dayoung.procurement.purchase.domain.PurchaseOrderLine;
import com.dayoung.procurement.purchase.exception.PurchaseOrderLineNotFoundException;
import com.dayoung.procurement.purchase.repository.PurchaseOrderLineRepository;
import com.dayoung.procurement.receipt.domain.GoodsReceiptStatus;
import com.dayoung.procurement.receipt.repository.GoodsReceiptLineRepository;
import com.dayoung.procurement.user.domain.AppUser;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ThreeWayMatchingService {

	private final PurchaseOrderLineRepository purchaseOrderLineRepository;
	private final GoodsReceiptLineRepository goodsReceiptLineRepository;
	private final InvoiceLineRepository invoiceLineRepository;
	private final ThreeWayMatchingPolicy matchingPolicy;
	private final MatchResultRepository matchResultRepository;
	private final AccrualEntryService accrualEntryService;

	public ThreeWayMatchingService(
			PurchaseOrderLineRepository purchaseOrderLineRepository,
			GoodsReceiptLineRepository goodsReceiptLineRepository,
			InvoiceLineRepository invoiceLineRepository,
			ThreeWayMatchingPolicy matchingPolicy,
			MatchResultRepository matchResultRepository,
			AccrualEntryService accrualEntryService
	) {
		this.purchaseOrderLineRepository = purchaseOrderLineRepository;
		this.goodsReceiptLineRepository = goodsReceiptLineRepository;
		this.invoiceLineRepository = invoiceLineRepository;
		this.matchingPolicy = matchingPolicy;
		this.matchResultRepository = matchResultRepository;
		this.accrualEntryService = accrualEntryService;
	}

	public MatchingStatus decide(Long purchaseOrderLineId) {
		return matchingPolicy.decide(calculateTotals(purchaseOrderLineId));
	}

	@Transactional
	public Long evaluateAndSave(Long purchaseOrderLineId) {
		return evaluate(purchaseOrderLineId).getId();
	}

	@Transactional
	public Long matchAndSettle(Long purchaseOrderLineId, LocalDate postingDate, AppUser createdBy) {
		MatchResult result = evaluate(purchaseOrderLineId);
		if (result.getStatus() == MatchingStatus.MATCHED) {
			PurchaseOrderLine orderLine = result.getPurchaseOrderLine();
			accrualEntryService.createForMatching(
					orderLine,
					result.getInvoicedAmount(),
					orderLine.getPurchaseOrder().getCurrency(),
					postingDate,
					createdBy
			);
		}
		return result.getId();
	}

	private MatchResult evaluate(Long purchaseOrderLineId) {
		PurchaseOrderLine orderLine = purchaseOrderLineRepository.findById(purchaseOrderLineId)
				.orElseThrow(() -> new PurchaseOrderLineNotFoundException(purchaseOrderLineId));
		MatchingLineTotals totals = calculateTotals(orderLine);
		MatchingStatus status = matchingPolicy.decide(totals);
		MatchResult result = matchResultRepository.findByPurchaseOrderLine_Id(purchaseOrderLineId)
				.orElseGet(() -> new MatchResult(orderLine));
		result.update(
				status,
				totals.orderedQuantity(),
				totals.receivedQuantity(),
				totals.invoicedQuantity(),
				totals.orderedAmount(),
				totals.invoicedAmount()
		);
		return matchResultRepository.save(result);
	}

	public MatchingLineTotals calculateTotals(Long purchaseOrderLineId) {
		PurchaseOrderLine orderLine = purchaseOrderLineRepository.findById(purchaseOrderLineId)
				.orElseThrow(() -> new PurchaseOrderLineNotFoundException(purchaseOrderLineId));
		return calculateTotals(orderLine);
	}

	private MatchingLineTotals calculateTotals(PurchaseOrderLine orderLine) {
		Long purchaseOrderLineId = orderLine.getId();
		return new MatchingLineTotals(
				orderLine.getId(),
				quantity(orderLine.getQuantity()),
				quantity(goodsReceiptLineRepository.sumQuantityByPurchaseOrderLineIdAndStatus(
						purchaseOrderLineId,
						GoodsReceiptStatus.POSTED
				)),
				quantity(invoiceLineRepository.sumQuantityByPurchaseOrderLineIdAndStatus(
						purchaseOrderLineId,
						InvoiceStatus.RECEIVED
				)),
				amount(orderLine.getSupplyAmount()),
				amount(invoiceLineRepository.sumSupplyAmountByPurchaseOrderLineIdAndStatus(
						purchaseOrderLineId,
						InvoiceStatus.RECEIVED
				))
		);
	}

	private BigDecimal quantity(BigDecimal value) {
		return value.setScale(3);
	}

	private BigDecimal amount(BigDecimal value) {
		return value.setScale(2);
	}
}
