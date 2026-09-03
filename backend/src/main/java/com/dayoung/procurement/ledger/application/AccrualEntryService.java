package com.dayoung.procurement.ledger.application;

import com.dayoung.procurement.invoice.domain.InvoiceLine;
import com.dayoung.procurement.invoice.domain.InvoiceStatus;
import com.dayoung.procurement.invoice.repository.InvoiceLineRepository;
import com.dayoung.procurement.ledger.domain.AccrualEntry;
import com.dayoung.procurement.ledger.domain.AccrualEntryType;
import com.dayoung.procurement.ledger.repository.AccrualEntryRepository;
import com.dayoung.procurement.purchase.domain.PurchaseOrderLine;
import com.dayoung.procurement.receipt.domain.GoodsReceipt;
import com.dayoung.procurement.receipt.domain.GoodsReceiptLine;
import com.dayoung.procurement.receipt.domain.GoodsReceiptStatus;
import com.dayoung.procurement.receipt.exception.GoodsReceiptNotFoundException;
import com.dayoung.procurement.receipt.exception.InvalidGoodsReceiptStateException;
import com.dayoung.procurement.receipt.repository.GoodsReceiptRepository;
import com.dayoung.procurement.user.domain.AppUser;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AccrualEntryService {

	private static final DateTimeFormatter PERIOD_FORMAT = DateTimeFormatter.ofPattern("yyyyMM");

	private final GoodsReceiptRepository goodsReceiptRepository;
	private final AccrualEntryRepository accrualEntryRepository;
	private final InvoiceLineRepository invoiceLineRepository;

	public AccrualEntryService(
			GoodsReceiptRepository goodsReceiptRepository,
			AccrualEntryRepository accrualEntryRepository,
			InvoiceLineRepository invoiceLineRepository
	) {
		this.goodsReceiptRepository = goodsReceiptRepository;
		this.accrualEntryRepository = accrualEntryRepository;
		this.invoiceLineRepository = invoiceLineRepository;
	}

	@Transactional
	public void createForGoodsReceipt(Long receiptId) {
		GoodsReceipt receipt = goodsReceiptRepository.findByIdForAccrual(receiptId)
				.orElseThrow(() -> new GoodsReceiptNotFoundException(receiptId));
		if (receipt.getStatus() != GoodsReceiptStatus.POSTED) {
			throw new InvalidGoodsReceiptStateException(receipt.getStatus());
		}

		for (GoodsReceiptLine receiptLine : receipt.getLines()) {
			if (accrualEntryRepository.existsByGoodsReceiptLine_IdAndEntryType(
					receiptLine.getId(),
					AccrualEntryType.GR_ACCRUAL
			)) {
				continue;
			}
			BigDecimal amount = receiptLine.getQuantity()
					.multiply(receiptLine.getPurchaseOrderLine().getUnitPrice())
					.setScale(2, RoundingMode.HALF_UP);
			AccrualEntry entry = new AccrualEntry(
					generateEntryNumber(),
					receiptLine.getPurchaseOrderLine(),
					receiptLine,
					amount,
					receipt.getPurchaseOrder().getCurrency(),
					receipt.getPostingDate(),
					receipt.getPostingDate().format(PERIOD_FORMAT),
					receipt.getReceivedBy()
			);
			accrualEntryRepository.save(entry);
		}
	}

	@Transactional
	public void createForMatching(PurchaseOrderLine orderLine) {
		List<InvoiceLine> invoiceLines = invoiceLineRepository
				.findAllByPurchaseOrderLine_IdAndInvoice_StatusOrderById(
						orderLine.getId(),
						InvoiceStatus.RECEIVED
				);
		for (InvoiceLine invoiceLine : invoiceLines) {
			if (accrualEntryRepository.existsByInvoiceLine_IdAndEntryType(
					invoiceLine.getId(),
					AccrualEntryType.INVOICE_MATCH
			)) {
				continue;
			}
			accrualEntryRepository.save(AccrualEntry.forInvoiceMatch(generateEntryNumber(), invoiceLine));
		}
	}

	@Transactional
	public void createCancellationOffsetsForGoodsReceipt(
			Long goodsReceiptId,
			LocalDate postingDate,
			AppUser createdBy
	) {
		createCancellationOffsets(
				accrualEntryRepository.findAllByGoodsReceiptLine_GoodsReceipt_IdOrderById(goodsReceiptId),
				postingDate,
				createdBy
		);
	}

	@Transactional
	public void createCancellationOffsetsForInvoice(Long invoiceId, LocalDate postingDate, AppUser createdBy) {
		createCancellationOffsets(
				accrualEntryRepository.findAllByInvoiceLine_Invoice_IdOrderById(invoiceId),
				postingDate,
				createdBy
		);
	}

	private void createCancellationOffsets(
			List<AccrualEntry> originals,
			LocalDate postingDate,
			AppUser createdBy
	) {
		for (AccrualEntry original : originals) {
			if (accrualEntryRepository.existsByReversalOf_Id(original.getId())) {
				continue;
			}
			accrualEntryRepository.save(AccrualEntry.cancellationOffset(
					generateEntryNumber(),
					original,
					postingDate,
					createdBy
			));
		}
	}

	private String generateEntryNumber() {
		return "AE-" + UUID.randomUUID().toString().toUpperCase(Locale.ROOT);
	}
}
