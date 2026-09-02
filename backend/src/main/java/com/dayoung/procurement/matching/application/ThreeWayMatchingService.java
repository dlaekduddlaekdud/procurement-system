package com.dayoung.procurement.matching.application;

import com.dayoung.procurement.invoice.domain.InvoiceStatus;
import com.dayoung.procurement.invoice.repository.InvoiceLineRepository;
import com.dayoung.procurement.matching.domain.MatchResult;
import com.dayoung.procurement.matching.domain.MatchingStatus;
import com.dayoung.procurement.matching.repository.MatchResultRepository;
import com.dayoung.procurement.purchase.domain.PurchaseOrderLine;
import com.dayoung.procurement.purchase.exception.PurchaseOrderLineNotFoundException;
import com.dayoung.procurement.purchase.repository.PurchaseOrderLineRepository;
import com.dayoung.procurement.receipt.domain.GoodsReceiptStatus;
import com.dayoung.procurement.receipt.repository.GoodsReceiptLineRepository;
import java.math.BigDecimal;
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

	public ThreeWayMatchingService(
			PurchaseOrderLineRepository purchaseOrderLineRepository,
			GoodsReceiptLineRepository goodsReceiptLineRepository,
			InvoiceLineRepository invoiceLineRepository,
			ThreeWayMatchingPolicy matchingPolicy,
			MatchResultRepository matchResultRepository
	) {
		this.purchaseOrderLineRepository = purchaseOrderLineRepository;
		this.goodsReceiptLineRepository = goodsReceiptLineRepository;
		this.invoiceLineRepository = invoiceLineRepository;
		this.matchingPolicy = matchingPolicy;
		this.matchResultRepository = matchResultRepository;
	}

	public MatchingStatus decide(Long purchaseOrderLineId) {
		return matchingPolicy.decide(calculateTotals(purchaseOrderLineId));
	}

	@Transactional
	public Long match(Long purchaseOrderLineId) {
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
		return matchResultRepository.save(result).getId();
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
