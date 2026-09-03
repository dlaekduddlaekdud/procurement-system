package com.dayoung.procurement.matching.repository;

import com.dayoung.procurement.matching.domain.MatchResult;
import com.dayoung.procurement.matching.domain.MatchingStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MatchResultRepository extends JpaRepository<MatchResult, Long> {

	Optional<MatchResult> findByPurchaseOrderLine_Id(Long purchaseOrderLineId);

	@Query("""
			select result
			from MatchResult result
			join fetch result.purchaseOrderLine
			where result.status in :statuses
			order by result.purchaseOrderLine.id
			""")
	List<MatchResult> findAllByStatusIn(@Param("statuses") Collection<MatchingStatus> statuses);
}
