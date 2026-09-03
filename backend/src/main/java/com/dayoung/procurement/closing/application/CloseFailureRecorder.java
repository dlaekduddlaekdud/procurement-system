package com.dayoung.procurement.closing.application;

import com.dayoung.procurement.closing.domain.ClosePeriod;
import com.dayoung.procurement.closing.domain.CloseRun;
import com.dayoung.procurement.closing.domain.CloseRunTriggerType;
import com.dayoung.procurement.closing.repository.ClosePeriodRepository;
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

	private final ClosePeriodRepository closePeriodRepository;
	private final CloseRunRepository closeRunRepository;
	private final AppUserRepository appUserRepository;

	public CloseFailureRecorder(
			ClosePeriodRepository closePeriodRepository,
			CloseRunRepository closeRunRepository,
			AppUserRepository appUserRepository
	) {
		this.closePeriodRepository = closePeriodRepository;
		this.closeRunRepository = closeRunRepository;
		this.appUserRepository = appUserRepository;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void recordManualFailure(String period, Long adminId, RuntimeException exception) {
		ClosePeriod closePeriod = closePeriodRepository.findByPeriodForUpdate(period)
				.orElseGet(() -> closePeriodRepository.saveAndFlush(new ClosePeriod(period)));
		AppUser admin = appUserRepository.getReferenceById(adminId);
		LocalDateTime now = LocalDateTime.now();
		CloseRun failedRun = new CloseRun(
				closePeriod,
				closeRunRepository.findNextAttemptNo(closePeriod.getId()),
				CloseRunTriggerType.MANUAL,
				now,
				admin
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
