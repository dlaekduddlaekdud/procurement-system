package com.dayoung.procurement.closing.application;

import com.dayoung.procurement.closing.domain.ClosePeriod;
import com.dayoung.procurement.closing.domain.ClosePeriodStatus;
import com.dayoung.procurement.closing.domain.CloseRun;
import com.dayoung.procurement.closing.domain.CloseRunTriggerType;
import com.dayoung.procurement.closing.repository.ClosePeriodRepository;
import com.dayoung.procurement.closing.repository.CloseRunRepository;
import com.dayoung.procurement.user.domain.AppUser;
import com.dayoung.procurement.user.repository.AppUserRepository;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CloseTransactionService {

	private final ClosePeriodRepository closePeriodRepository;
	private final CloseRunRepository closeRunRepository;
	private final AppUserRepository appUserRepository;
	private final CloseSuccessRecorder closeSuccessRecorder;

	public CloseTransactionService(
			ClosePeriodRepository closePeriodRepository,
			CloseRunRepository closeRunRepository,
			AppUserRepository appUserRepository,
			CloseSuccessRecorder closeSuccessRecorder
	) {
		this.closePeriodRepository = closePeriodRepository;
		this.closeRunRepository = closeRunRepository;
		this.appUserRepository = appUserRepository;
		this.closeSuccessRecorder = closeSuccessRecorder;
	}

	@Transactional
	public Long closeManually(String period, Long adminId) {
		ClosePeriod closePeriod = closePeriodRepository.findByPeriodForUpdate(period)
				.orElseGet(() -> closePeriodRepository.saveAndFlush(new ClosePeriod(period)));
		AppUser admin = appUserRepository.getReferenceById(adminId);
		LocalDateTime startedAt = LocalDateTime.now();
		CloseRun closeRun = new CloseRun(
				closePeriod,
				closeRunRepository.findNextAttemptNo(closePeriod.getId()),
				CloseRunTriggerType.MANUAL,
				startedAt,
				admin
		);

		if (closePeriod.getStatus() == ClosePeriodStatus.CLOSED) {
			closeRun.skip(LocalDateTime.now());
			return closeRunRepository.save(closeRun).getId();
		}
		return closeSuccessRecorder.complete(closePeriod, closeRun);
	}
}
