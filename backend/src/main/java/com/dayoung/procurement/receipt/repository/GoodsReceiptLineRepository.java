package com.dayoung.procurement.receipt.repository;

import com.dayoung.procurement.receipt.domain.GoodsReceiptLine;
import com.dayoung.procurement.receipt.domain.GoodsReceiptStatus;
import java.math.BigDecimal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GoodsReceiptLineRepository extends JpaRepository<GoodsReceiptLine, Long> {

	@Query("""
			select coalesce(sum(line.quantity), 0)
			from GoodsReceiptLine line
			where line.purchaseOrderLine.id = :purchaseOrderLineId
			  and line.goodsReceipt.status = :status
			""")
	BigDecimal sumQuantityByPurchaseOrderLineIdAndStatus(
			@Param("purchaseOrderLineId") Long purchaseOrderLineId,
			@Param("status") GoodsReceiptStatus status
	);
}
