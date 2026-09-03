package com.dayoung.procurement.ledger.application;

import com.dayoung.procurement.closing.application.ClosePeriodService;
import com.dayoung.procurement.ledger.domain.AccrualEntry;
import com.dayoung.procurement.ledger.domain.AccrualEntryType;
import com.dayoung.procurement.ledger.exception.AccrualEntryNotFoundException;
import com.dayoung.procurement.ledger.exception.DuplicateAccrualReversalException;
import com.dayoung.procurement.ledger.exception.InvalidAccrualReversalTargetException;
import com.dayoung.procurement.ledger.repository.AccrualEntryRepository;
import com.dayoung.procurement.purchase.exception.InactivePurchaseUserException;
import com.dayoung.procurement.purchase.exception.PurchaseRoleRequiredException;
import com.dayoung.procurement.user.domain.AppUser;
import com.dayoung.procurement.user.domain.RoleCode;
import com.dayoung.procurement.user.repository.AppUserRepository;
import com.dayoung.procurement.user.repository.UserRoleRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@Transactional(readOnly = true)
public class LedgerCorrectionService {

	private final AccrualEntryRepository accrualEntryRepository;
	private final ClosePeriodService closePeriodService;
	private final AppUserRepository appUserRepository;
	private final UserRoleRepository userRoleRepository;

	public LedgerCorrectionService(
			AccrualEntryRepository accrualEntryRepository,
			ClosePeriodService closePeriodService,
			AppUserRepository appUserRepository,
			UserRoleRepository userRoleRepository
	) {
		this.accrualEntryRepository = accrualEntryRepository;
		this.closePeriodService = closePeriodService;
		this.appUserRepository = appUserRepository;
		this.userRoleRepository = userRoleRepository;
	}

	@Transactional
	public Long reverse(
			@NotNull Long entryId,
			@NotNull Long adminId,
			@NotNull @Valid ReverseAccrualEntryCommand command
	) {
		AppUser admin = getActiveAdmin(adminId);
		AccrualEntry original = accrualEntryRepository.findByIdForUpdate(entryId)
				.orElseThrow(() -> new AccrualEntryNotFoundException(entryId));
		validateOriginalEntry(original);
		closePeriodService.requireClosed(original.getPostingDate());
		closePeriodService.requireOpen(command.postingDate());
		if (accrualEntryRepository.existsByReversalOf_Id(entryId)) {
			throw new DuplicateAccrualReversalException(entryId);
		}

		AccrualEntry reversal = AccrualEntry.reversal(
				generateEntryNumber(),
				original,
				command.postingDate(),
				admin
		);
		return accrualEntryRepository.save(reversal).getId();
	}

	private void validateOriginalEntry(AccrualEntry entry) {
		if (entry.getEntryType() != AccrualEntryType.GR_ACCRUAL
				&& entry.getEntryType() != AccrualEntryType.INVOICE_MATCH) {
			throw new InvalidAccrualReversalTargetException(entry.getId());
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

	private String generateEntryNumber() {
		return "AE-" + UUID.randomUUID().toString().toUpperCase(Locale.ROOT);
	}
}
