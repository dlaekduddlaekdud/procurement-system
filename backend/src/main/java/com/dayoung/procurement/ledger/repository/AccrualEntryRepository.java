package com.dayoung.procurement.ledger.repository;

import com.dayoung.procurement.ledger.domain.AccrualEntry;
import com.dayoung.procurement.ledger.domain.AccrualEntryType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccrualEntryRepository extends JpaRepository<AccrualEntry, Long> {

	boolean existsByGoodsReceiptLine_IdAndEntryType(Long goodsReceiptLineId, AccrualEntryType entryType);

	boolean existsByInvoiceLine_IdAndEntryType(Long invoiceLineId, AccrualEntryType entryType);

	boolean existsByReversalOf_Id(Long accrualEntryId);

	List<AccrualEntry> findAllByGoodsReceiptLine_GoodsReceipt_IdOrderById(Long goodsReceiptId);

	List<AccrualEntry> findAllByInvoiceLine_Invoice_IdOrderById(Long invoiceId);

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

	@Query("""
			select entry.purchaseOrderLine as purchaseOrderLine,
			       entry.currency as currency,
			       sum(entry.amount) as balanceAmount
			from AccrualEntry entry
			where entry.postingDate <= :periodEnd
			group by entry.purchaseOrderLine, entry.currency
			having sum(entry.amount) <> 0
			order by entry.purchaseOrderLine.id
			""")
	List<AccrualBalanceView> findOutstandingBalancesThrough(@Param("periodEnd") LocalDate periodEnd);
}
