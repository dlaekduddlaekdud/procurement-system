package com.dayoung.procurement.closing.application;

import com.dayoung.procurement.closing.domain.ClosePeriod;
import com.dayoung.procurement.closing.domain.ClosePeriodStatus;
import com.dayoung.procurement.closing.domain.CloseRun;
import com.dayoung.procurement.closing.domain.CloseRunTriggerType;
import com.dayoung.procurement.closing.repository.CloseRunRepository;
import com.dayoung.procurement.user.domain.AppUser;
import com.dayoung.procurement.user.repository.AppUserRepository;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CloseTransactionService {

	private final ClosePeriodLockService closePeriodLockService;
	private final CloseRunRepository closeRunRepository;
	private final AppUserRepository appUserRepository;
	private final CloseSuccessRecorder closeSuccessRecorder;
	private final CloseSnapshotService closeSnapshotService;

	public CloseTransactionService(
			ClosePeriodLockService closePeriodLockService,
			CloseRunRepository closeRunRepository,
			AppUserRepository appUserRepository,
			CloseSuccessRecorder closeSuccessRecorder,
			CloseSnapshotService closeSnapshotService
	) {
		this.closePeriodLockService = closePeriodLockService;
		this.closeRunRepository = closeRunRepository;
		this.appUserRepository = appUserRepository;
		this.closeSuccessRecorder = closeSuccessRecorder;
		this.closeSnapshotService = closeSnapshotService;
	}

	@Transactional
	public Long closeManually(String period, Long adminId) {
		AppUser admin = appUserRepository.getReferenceById(adminId);
		return close(period, CloseRunTriggerType.MANUAL, admin);
	}

	@Transactional
	public Long closeAutomatically(String period) {
		return close(period, CloseRunTriggerType.SCHEDULED, null);
	}

	private Long close(String period, CloseRunTriggerType triggerType, AppUser requestedBy) {
		ClosePeriod closePeriod = closePeriodLockService.getOrCreateForUpdate(period);
		LocalDateTime startedAt = LocalDateTime.now();
		CloseRun closeRun = new CloseRun(
				closePeriod,
				closeRunRepository.findNextAttemptNo(closePeriod.getId()),
				triggerType,
				startedAt,
				requestedBy
		);

		if (closePeriod.getStatus() == ClosePeriodStatus.CLOSED) {
			closeRun.skip(LocalDateTime.now());
			return closeRunRepository.save(closeRun).getId();
		}
		closeRunRepository.saveAndFlush(closeRun);
		closeSnapshotService.createSnapshots(closePeriod, closeRun);
		return closeSuccessRecorder.complete(closePeriod, closeRun);
	}
}
