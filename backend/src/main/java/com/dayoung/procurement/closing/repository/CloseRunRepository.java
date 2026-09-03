package com.dayoung.procurement.closing.repository;

import com.dayoung.procurement.closing.domain.CloseRun;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CloseRunRepository extends JpaRepository<CloseRun, Long> {

	List<CloseRun> findAllByClosePeriod_IdOrderByAttemptNo(Long closePeriodId);

	Optional<CloseRun> findTopByClosePeriod_IdOrderByAttemptNoDesc(Long closePeriodId);
}
