package com.dayoung.procurement.purchase.repository;

import com.dayoung.procurement.purchase.domain.PurchaseRequest;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseRequestRepository extends JpaRepository<PurchaseRequest, Long> {

	Optional<PurchaseRequest> findByRequestNumber(String requestNumber);

	List<PurchaseRequest> findAllByRequester_IdOrderByCreatedAtDesc(Long requesterId);

	boolean existsByRequestNumber(String requestNumber);
}
