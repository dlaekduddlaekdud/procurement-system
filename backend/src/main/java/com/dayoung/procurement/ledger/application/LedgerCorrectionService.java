package com.dayoung.procurement.ledger.application;

import com.dayoung.procurement.audit.application.AuditLogService;
import com.dayoung.procurement.audit.domain.AuditEventType;
import com.dayoung.procurement.audit.domain.AuditTargetType;
import com.dayoung.procurement.closing.application.ClosePeriodService;
import com.dayoung.procurement.ledger.domain.AccrualEntry;
import com.dayoung.procurement.ledger.domain.AccrualEntryType;
import com.dayoung.procurement.ledger.exception.AccrualEntryNotFoundException;
import com.dayoung.procurement.ledger.exception.DuplicateAccrualReversalException;
import com.dayoung.procurement.ledger.exception.DuplicateAccrualRepostingException;
import com.dayoung.procurement.ledger.exception.InvalidAccrualReversalTargetException;
import com.dayoung.procurement.ledger.exception.InvalidAccrualRepostingTargetException;
import com.dayoung.procurement.ledger.repository.AccrualEntryRepository;
import com.dayoung.procurement.purchase.exception.InactivePurchaseUserException;
import com.dayoung.procurement.purchase.exception.PurchaseRoleRequiredException;
import com.dayoung.procurement.user.domain.AppUser;
import com.dayoung.procurement.user.domain.RoleCode;
import com.dayoung.procurement.user.repository.AppUserRepository;
import com.dayoung.procurement.user.repository.UserRoleRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
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
	private final AuditLogService auditLogService;

	public LedgerCorrectionService(
			AccrualEntryRepository accrualEntryRepository,
			ClosePeriodService closePeriodService,
			AppUserRepository appUserRepository,
			UserRoleRepository userRoleRepository,
			AuditLogService auditLogService
	) {
		this.accrualEntryRepository = accrualEntryRepository;
		this.closePeriodService = closePeriodService;
		this.appUserRepository = appUserRepository;
		this.userRoleRepository = userRoleRepository;
		this.auditLogService = auditLogService;
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
		Long reversalId = accrualEntryRepository.save(reversal).getId();
		auditLogService.record(
				AuditEventType.ACCRUAL_REVERSED,
				AuditTargetType.ACCRUAL_ENTRY,
				reversalId,
				admin,
				command.reason()
		);
		return reversalId;
	}

	@Transactional
	public Long repost(
			@NotNull Long reversalId,
			@NotNull Long adminId,
			@NotNull @Valid RepostAccrualEntryCommand command
	) {
		AppUser admin = getActiveAdmin(adminId);
		AccrualEntry reversal = accrualEntryRepository.findByIdForUpdate(reversalId)
				.orElseThrow(() -> new AccrualEntryNotFoundException(reversalId));
		if (reversal.getEntryType() != AccrualEntryType.REVERSAL) {
			throw new InvalidAccrualRepostingTargetException(reversalId);
		}
		closePeriodService.requireOpen(command.postingDate());
		if (accrualEntryRepository.existsByRepostingOf_Id(reversalId)) {
			throw new DuplicateAccrualRepostingException(reversalId);
		}

		BigDecimal originalAmount = reversal.getReversalOf().getAmount();
		BigDecimal correctedAmount = command.amount().multiply(BigDecimal.valueOf(originalAmount.signum()));
		AccrualEntry correction = AccrualEntry.correction(
				generateEntryNumber(),
				reversal,
				correctedAmount,
				command.postingDate(),
				admin
		);
		Long correctionId = accrualEntryRepository.save(correction).getId();
		auditLogService.record(
				AuditEventType.ACCRUAL_REPOSTED,
				AuditTargetType.ACCRUAL_ENTRY,
				correctionId,
				admin,
				command.reason()
		);
		return correctionId;
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
