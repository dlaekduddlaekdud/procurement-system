package com.dayoung.procurement.ledger.repository;

import com.dayoung.procurement.ledger.domain.AccrualEntry;
import com.dayoung.procurement.ledger.domain.AccrualEntryType;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccrualEntryRepository extends JpaRepository<AccrualEntry, Long> {

	boolean existsByGoodsReceiptLine_IdAndEntryType(Long goodsReceiptLineId, AccrualEntryType entryType);

	List<AccrualEntry> findAllByGoodsReceiptLine_GoodsReceipt_IdOrderById(Long goodsReceiptId);

	List<AccrualEntry> findAllByPurchaseOrderLine_IdOrderById(Long purchaseOrderLineId);

	@Query("""
			select coalesce(sum(entry.amount), 0)
			from AccrualEntry entry
			where entry.purchaseOrderLine.id = :purchaseOrderLineId
			  and entry.entryType = :entryType
			""")
	BigDecimal sumAmountByPurchaseOrderLineIdAndEntryType(
			@Param("purchaseOrderLineId") Long purchaseOrderLineId,
			@Param("entryType") AccrualEntryType entryType
	);
}
