package com.dayoung.procurement.ledger.repository;

import com.dayoung.procurement.ledger.domain.AccrualEntry;
import com.dayoung.procurement.ledger.domain.AccrualEntryType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccrualEntryRepository extends JpaRepository<AccrualEntry, Long> {

	boolean existsByGoodsReceiptLine_IdAndEntryType(Long goodsReceiptLineId, AccrualEntryType entryType);

	List<AccrualEntry> findAllByGoodsReceiptLine_GoodsReceipt_IdOrderById(Long goodsReceiptId);
}
