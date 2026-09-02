package com.dayoung.procurement.purchase.repository;

import com.dayoung.procurement.purchase.domain.PurchaseOrderLine;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseOrderLineRepository extends JpaRepository<PurchaseOrderLine, Long> {

	List<PurchaseOrderLine> findAllByPurchaseOrder_IdOrderByLineNumber(Long purchaseOrderId);

	boolean existsByPurchaseRequestLine_Id(Long purchaseRequestLineId);
}
