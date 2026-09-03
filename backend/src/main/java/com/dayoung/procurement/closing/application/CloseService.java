package com.dayoung.procurement.closing.application;

import com.dayoung.procurement.purchase.exception.InactivePurchaseUserException;
import com.dayoung.procurement.purchase.exception.PurchaseRoleRequiredException;
import com.dayoung.procurement.user.domain.AppUser;
import com.dayoung.procurement.user.domain.RoleCode;
import com.dayoung.procurement.user.repository.AppUserRepository;
import com.dayoung.procurement.user.repository.UserRoleRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class CloseService {

	private final AppUserRepository appUserRepository;
	private final UserRoleRepository userRoleRepository;
	private final CloseTransactionService closeTransactionService;
	private final CloseFailureRecorder closeFailureRecorder;

	public CloseService(
			AppUserRepository appUserRepository,
			UserRoleRepository userRoleRepository,
			CloseTransactionService closeTransactionService,
			CloseFailureRecorder closeFailureRecorder
	) {
		this.appUserRepository = appUserRepository;
		this.userRoleRepository = userRoleRepository;
		this.closeTransactionService = closeTransactionService;
		this.closeFailureRecorder = closeFailureRecorder;
	}

	public Long closeManually(
			@NotBlank @Pattern(regexp = "\\d{6}") String period,
			@NotNull Long adminId
	) {
		getActiveAdmin(adminId);
		try {
			return closeTransactionService.closeManually(period, adminId);
		} catch (RuntimeException exception) {
			closeFailureRecorder.recordManualFailure(period, adminId, exception);
			throw exception;
		}
	}

	public Long closeAutomatically(
			@NotBlank @Pattern(regexp = "\\d{6}") String period
	) {
		try {
			return closeTransactionService.closeAutomatically(period);
		} catch (RuntimeException exception) {
			closeFailureRecorder.recordScheduledFailure(period, exception);
			throw exception;
		}
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
