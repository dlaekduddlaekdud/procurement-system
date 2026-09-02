package com.dayoung.procurement.purchase.repository;

import com.dayoung.procurement.purchase.domain.PurchaseOrder;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

	Optional<PurchaseOrder> findByOrderNumber(String orderNumber);

	Optional<PurchaseOrder> findByPurchaseRequest_Id(Long purchaseRequestId);

	boolean existsByPurchaseRequest_Id(Long purchaseRequestId);
}
