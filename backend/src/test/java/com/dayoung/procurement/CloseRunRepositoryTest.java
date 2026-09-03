package com.dayoung.procurement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.dayoung.procurement.closing.domain.ClosePeriod;
import com.dayoung.procurement.closing.domain.CloseRun;
import com.dayoung.procurement.closing.domain.CloseRunStatus;
import com.dayoung.procurement.closing.domain.CloseRunTriggerType;
import com.dayoung.procurement.closing.repository.ClosePeriodRepository;
import com.dayoung.procurement.closing.repository.CloseRunRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class CloseRunRepositoryTest {

	@Autowired
	private ClosePeriodRepository closePeriodRepository;

	@Autowired
	private CloseRunRepository closeRunRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void savesScheduledCloseRunAndCompletesIt() {
		ClosePeriod closePeriod = closePeriodRepository.save(new ClosePeriod("202609"));
		LocalDateTime startedAt = LocalDateTime.of(2026, 9, 30, 23, 50);
		CloseRun closeRun = new CloseRun(
				closePeriod,
				1,
				CloseRunTriggerType.SCHEDULED,
				startedAt,
				null
		);
		closeRun.succeed(startedAt.plusMinutes(5));
		Long closeRunId = closeRunRepository.save(closeRun).getId();

		entityManager.flush();
		entityManager.clear();

		CloseRun saved = closeRunRepository.findById(closeRunId).orElseThrow();
		assertEquals(1, saved.getAttemptNo());
		assertEquals(CloseRunStatus.SUCCESS, saved.getStatus());
		assertEquals(CloseRunTriggerType.SCHEDULED, saved.getTriggerType());
		assertEquals(startedAt, saved.getStartedAt());
		assertEquals(startedAt.plusMinutes(5), saved.getCompletedAt());
		assertNull(saved.getRequestedBy());
		assertNull(saved.getFailureMessage());
	}
}
