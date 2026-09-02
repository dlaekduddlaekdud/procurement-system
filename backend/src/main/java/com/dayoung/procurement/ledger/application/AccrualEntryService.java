package com.dayoung.procurement.ledger.application;

import com.dayoung.procurement.ledger.domain.AccrualEntry;
import com.dayoung.procurement.ledger.domain.AccrualEntryType;
import com.dayoung.procurement.ledger.repository.AccrualEntryRepository;
import com.dayoung.procurement.receipt.domain.GoodsReceipt;
import com.dayoung.procurement.receipt.domain.GoodsReceiptLine;
import com.dayoung.procurement.receipt.domain.GoodsReceiptStatus;
import com.dayoung.procurement.receipt.exception.GoodsReceiptNotFoundException;
import com.dayoung.procurement.receipt.exception.InvalidGoodsReceiptStateException;
import com.dayoung.procurement.receipt.repository.GoodsReceiptRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
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

	public AccrualEntryService(
			GoodsReceiptRepository goodsReceiptRepository,
			AccrualEntryRepository accrualEntryRepository
	) {
		this.goodsReceiptRepository = goodsReceiptRepository;
		this.accrualEntryRepository = accrualEntryRepository;
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

	private String generateEntryNumber() {
		return "AE-" + UUID.randomUUID().toString().toUpperCase(Locale.ROOT);
	}
}
