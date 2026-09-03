package com.dayoung.procurement.purchase.repository;

import com.dayoung.procurement.purchase.domain.PurchaseRequestLine;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseRequestLineRepository extends JpaRepository<PurchaseRequestLine, Long> {

	List<PurchaseRequestLine> findAllByPurchaseRequest_IdOrderByLineNumber(Long purchaseRequestId);
}
