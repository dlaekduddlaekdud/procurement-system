package com.dayoung.procurement.closing.repository;

import com.dayoung.procurement.closing.domain.ClosePeriod;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClosePeriodRepository extends JpaRepository<ClosePeriod, Long> {

	Optional<ClosePeriod> findByPeriod(String period);

	@Modifying(flushAutomatically = true)
	@Query(value = """
			insert ignore into close_period (period, status, version)
			values (:period, 'OPEN', 0)
			""", nativeQuery = true)
	int createIfAbsent(@Param("period") String period);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select closePeriod from ClosePeriod closePeriod where closePeriod.period = :period")
	Optional<ClosePeriod> findByPeriodForUpdate(@Param("period") String period);
}
