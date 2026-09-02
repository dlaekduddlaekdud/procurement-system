package com.dayoung.procurement.matching.application;

import com.dayoung.procurement.invoice.domain.InvoiceStatus;
import com.dayoung.procurement.invoice.repository.InvoiceLineRepository;
import com.dayoung.procurement.purchase.domain.PurchaseOrderLine;
import com.dayoung.procurement.purchase.exception.PurchaseOrderLineNotFoundException;
import com.dayoung.procurement.purchase.repository.PurchaseOrderLineRepository;
import com.dayoung.procurement.receipt.domain.GoodsReceiptStatus;
import com.dayoung.procurement.receipt.repository.GoodsReceiptLineRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ThreeWayMatchingService {

	private final PurchaseOrderLineRepository purchaseOrderLineRepository;
	private final GoodsReceiptLineRepository goodsReceiptLineRepository;
	private final InvoiceLineRepository invoiceLineRepository;

	public ThreeWayMatchingService(
			PurchaseOrderLineRepository purchaseOrderLineRepository,
			GoodsReceiptLineRepository goodsReceiptLineRepository,
			InvoiceLineRepository invoiceLineRepository
	) {
		this.purchaseOrderLineRepository = purchaseOrderLineRepository;
		this.goodsReceiptLineRepository = goodsReceiptLineRepository;
		this.invoiceLineRepository = invoiceLineRepository;
	}

	public MatchingLineTotals calculateTotals(Long purchaseOrderLineId) {
		PurchaseOrderLine orderLine = purchaseOrderLineRepository.findById(purchaseOrderLineId)
				.orElseThrow(() -> new PurchaseOrderLineNotFoundException(purchaseOrderLineId));
		return new MatchingLineTotals(
				orderLine.getId(),
				orderLine.getQuantity(),
				goodsReceiptLineRepository.sumQuantityByPurchaseOrderLineIdAndStatus(
						purchaseOrderLineId,
						GoodsReceiptStatus.POSTED
				),
				invoiceLineRepository.sumQuantityByPurchaseOrderLineIdAndStatus(
						purchaseOrderLineId,
						InvoiceStatus.RECEIVED
				),
				orderLine.getSupplyAmount(),
				invoiceLineRepository.sumSupplyAmountByPurchaseOrderLineIdAndStatus(
						purchaseOrderLineId,
						InvoiceStatus.RECEIVED
				)
		);
	}
}
