package com.dayoung.procurement.closing.application;

import com.dayoung.procurement.closing.domain.ClosePeriod;
import com.dayoung.procurement.closing.domain.CloseRun;
import com.dayoung.procurement.closing.domain.CloseRunTriggerType;
import com.dayoung.procurement.closing.repository.CloseRunRepository;
import com.dayoung.procurement.user.domain.AppUser;
import com.dayoung.procurement.user.repository.AppUserRepository;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CloseFailureRecorder {

	private static final int MAX_FAILURE_MESSAGE_LENGTH = 1000;

	private final ClosePeriodLockService closePeriodLockService;
	private final CloseRunRepository closeRunRepository;
	private final AppUserRepository appUserRepository;

	public CloseFailureRecorder(
			ClosePeriodLockService closePeriodLockService,
			CloseRunRepository closeRunRepository,
			AppUserRepository appUserRepository
	) {
		this.closePeriodLockService = closePeriodLockService;
		this.closeRunRepository = closeRunRepository;
		this.appUserRepository = appUserRepository;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void recordManualFailure(String period, Long adminId, RuntimeException exception) {
		AppUser admin = appUserRepository.getReferenceById(adminId);
		recordFailure(period, CloseRunTriggerType.MANUAL, admin, exception);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void recordScheduledFailure(String period, RuntimeException exception) {
		recordFailure(period, CloseRunTriggerType.SCHEDULED, null, exception);
	}

	private void recordFailure(
			String period,
			CloseRunTriggerType triggerType,
			AppUser requestedBy,
			RuntimeException exception
	) {
		ClosePeriod closePeriod = closePeriodLockService.getOrCreateForUpdate(period);
		LocalDateTime now = LocalDateTime.now();
		CloseRun failedRun = new CloseRun(
				closePeriod,
				closeRunRepository.findNextAttemptNo(closePeriod.getId()),
				triggerType,
				now,
				requestedBy
		);
		failedRun.fail(now, failureMessage(exception));
		closeRunRepository.save(failedRun);
	}

	private String failureMessage(RuntimeException exception) {
		String message = exception.getMessage();
		if (message == null || message.isBlank()) {
			message = exception.getClass().getSimpleName();
		}
		return message.substring(0, Math.min(message.length(), MAX_FAILURE_MESSAGE_LENGTH));
	}
}
