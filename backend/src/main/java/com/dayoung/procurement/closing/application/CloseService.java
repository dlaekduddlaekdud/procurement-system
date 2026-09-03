package com.dayoung.procurement.closing.application;

import com.dayoung.procurement.closing.domain.ClosePeriod;
import com.dayoung.procurement.closing.domain.ClosePeriodStatus;
import com.dayoung.procurement.closing.domain.CloseRun;
import com.dayoung.procurement.closing.domain.CloseRunTriggerType;
import com.dayoung.procurement.closing.repository.ClosePeriodRepository;
import com.dayoung.procurement.closing.repository.CloseRunRepository;
import com.dayoung.procurement.purchase.exception.InactivePurchaseUserException;
import com.dayoung.procurement.purchase.exception.PurchaseRoleRequiredException;
import com.dayoung.procurement.user.domain.AppUser;
import com.dayoung.procurement.user.domain.RoleCode;
import com.dayoung.procurement.user.repository.AppUserRepository;
import com.dayoung.procurement.user.repository.UserRoleRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@Transactional(readOnly = true)
public class CloseService {

	private final ClosePeriodRepository closePeriodRepository;
	private final CloseRunRepository closeRunRepository;
	private final AppUserRepository appUserRepository;
	private final UserRoleRepository userRoleRepository;

	public CloseService(
			ClosePeriodRepository closePeriodRepository,
			CloseRunRepository closeRunRepository,
			AppUserRepository appUserRepository,
			UserRoleRepository userRoleRepository
	) {
		this.closePeriodRepository = closePeriodRepository;
		this.closeRunRepository = closeRunRepository;
		this.appUserRepository = appUserRepository;
		this.userRoleRepository = userRoleRepository;
	}

	@Transactional
	public Long closeManually(
			@NotBlank @Pattern(regexp = "\\d{6}") String period,
			@NotNull Long adminId
	) {
		AppUser admin = getActiveAdmin(adminId);
		ClosePeriod closePeriod = closePeriodRepository.findByPeriodForUpdate(period)
				.orElseGet(() -> closePeriodRepository.saveAndFlush(new ClosePeriod(period)));
		int attemptNo = nextAttemptNo(closePeriod.getId());
		LocalDateTime startedAt = LocalDateTime.now();
		CloseRun closeRun = new CloseRun(
				closePeriod,
				attemptNo,
				CloseRunTriggerType.MANUAL,
				startedAt,
				admin
		);

		if (closePeriod.getStatus() == ClosePeriodStatus.CLOSED) {
			closeRun.skip(LocalDateTime.now());
		} else {
			LocalDateTime completedAt = LocalDateTime.now();
			closePeriod.close(completedAt);
			closeRun.succeed(completedAt);
		}
		return closeRunRepository.save(closeRun).getId();
	}

	private int nextAttemptNo(Long closePeriodId) {
		return closeRunRepository.findTopByClosePeriod_IdOrderByAttemptNoDesc(closePeriodId)
				.map(CloseRun::getAttemptNo)
				.map(attemptNo -> attemptNo + 1)
				.orElse(1);
	}

	private AppUser getActiveAdmin(Long adminId) {
		AppUser admin = appUserRepository.findById(adminId)
				.filter(AppUser::isActive)
				.orElseThrow(() -> new InactivePurchaseUserException(adminId));
		if (!userRoleRepository.existsByAppUser_IdAndRole_Code(adminId, RoleCode.ADMIN)) {
			throw new PurchaseRoleRequiredException(RoleCode.ADMIN);
		}
		return admin;
	}
}
