package com.dayoung.procurement.purchase.repository;

import com.dayoung.procurement.purchase.domain.PurchaseOrder;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

	Optional<PurchaseOrder> findByOrderNumber(String orderNumber);

	Optional<PurchaseOrder> findByPurchaseRequest_Id(Long purchaseRequestId);

	boolean existsByPurchaseRequest_Id(Long purchaseRequestId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select purchaseOrder from PurchaseOrder purchaseOrder where purchaseOrder.id = :orderId")
	Optional<PurchaseOrder> findByIdForReceipt(@Param("orderId") Long orderId);
}
