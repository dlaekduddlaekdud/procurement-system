package com.dayoung.procurement.closing.application;

import com.dayoung.procurement.closing.domain.ClosePeriod;
import com.dayoung.procurement.closing.domain.CloseRun;
import com.dayoung.procurement.closing.repository.CloseRunRepository;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
public class CloseSuccessRecorder {

	private final CloseRunRepository closeRunRepository;

	public CloseSuccessRecorder(CloseRunRepository closeRunRepository) {
		this.closeRunRepository = closeRunRepository;
	}

	public Long complete(ClosePeriod closePeriod, CloseRun closeRun) {
		LocalDateTime completedAt = LocalDateTime.now();
		closePeriod.close(completedAt);
		closeRun.succeed(completedAt);
		return closeRunRepository.saveAndFlush(closeRun).getId();
	}
}
