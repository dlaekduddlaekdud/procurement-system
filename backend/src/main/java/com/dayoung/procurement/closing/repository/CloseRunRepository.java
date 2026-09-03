package com.dayoung.procurement.closing.repository;

import com.dayoung.procurement.closing.domain.CloseRun;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CloseRunRepository extends JpaRepository<CloseRun, Long> {

	List<CloseRun> findAllByClosePeriod_IdOrderByAttemptNo(Long closePeriodId);

	@Query("""
			select coalesce(max(closeRun.attemptNo), 0) + 1
			from CloseRun closeRun
			where closeRun.closePeriod.id = :closePeriodId
			""")
	int findNextAttemptNo(@Param("closePeriodId") Long closePeriodId);
}
