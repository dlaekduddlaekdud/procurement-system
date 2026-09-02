package com.dayoung.procurement.matching.repository;

import com.dayoung.procurement.matching.domain.MatchResult;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchResultRepository extends JpaRepository<MatchResult, Long> {

	Optional<MatchResult> findByPurchaseOrderLine_Id(Long purchaseOrderLineId);
}
