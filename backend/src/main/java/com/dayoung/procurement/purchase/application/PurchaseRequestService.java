package com.dayoung.procurement.purchase.application;

import com.dayoung.procurement.masterdata.domain.Department;
import com.dayoung.procurement.masterdata.domain.Item;
import com.dayoung.procurement.masterdata.repository.ItemRepository;
import com.dayoung.procurement.purchase.domain.PurchaseRequest;
import com.dayoung.procurement.purchase.domain.PurchaseRequestLine;
import com.dayoung.procurement.purchase.exception.InactivePurchaseUserException;
import com.dayoung.procurement.purchase.exception.PurchaseItemNotFoundException;
import com.dayoung.procurement.purchase.exception.PurchaseRequestAccessDeniedException;
import com.dayoung.procurement.purchase.exception.PurchaseRequestNotFoundException;
import com.dayoung.procurement.purchase.exception.PurchaseRoleRequiredException;
import com.dayoung.procurement.purchase.repository.PurchaseRequestRepository;
import com.dayoung.procurement.user.domain.AppUser;
import com.dayoung.procurement.user.domain.RoleCode;
import com.dayoung.procurement.user.repository.AppUserRepository;
import com.dayoung.procurement.user.repository.UserRoleRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@Transactional(readOnly = true)
public class PurchaseRequestService {

	private final PurchaseRequestRepository purchaseRequestRepository;
	private final AppUserRepository appUserRepository;
	private final UserRoleRepository userRoleRepository;
	private final ItemRepository itemRepository;
	private final PurchaseRequestNumberGenerator numberGenerator;

	public PurchaseRequestService(
			PurchaseRequestRepository purchaseRequestRepository,
			AppUserRepository appUserRepository,
			UserRoleRepository userRoleRepository,
			ItemRepository itemRepository,
			PurchaseRequestNumberGenerator numberGenerator
	) {
		this.purchaseRequestRepository = purchaseRequestRepository;
		this.appUserRepository = appUserRepository;
		this.userRoleRepository = userRoleRepository;
		this.itemRepository = itemRepository;
		this.numberGenerator = numberGenerator;
	}

	@Transactional
	public Long create(@NotNull Long requesterId, @NotNull @Valid CreatePurchaseRequestCommand command) {
		AppUser requester = getActiveUser(requesterId);
		requireRole(requesterId, RoleCode.REQUESTER);
		Department department = requester.getDepartment();
		if (department == null || !department.isActive()) {
			throw new IllegalStateException("활성 상태의 소속 부서가 필요합니다.");
		}

		LocalDate requestDate = LocalDate.now();
		PurchaseRequest request = new PurchaseRequest(
				numberGenerator.generate(requestDate),
				requester,
				department,
				command.title(),
				command.purpose(),
				requestDate,
				command.neededDate()
		);

		for (CreatePurchaseRequestLineCommand lineCommand : command.lines()) {
			Item item = itemRepository.findById(lineCommand.itemId())
					.filter(Item::isActive)
					.orElseThrow(() -> new PurchaseItemNotFoundException(lineCommand.itemId()));
			BigDecimal estimatedAmount = lineCommand.quantity()
					.multiply(lineCommand.estimatedUnitPrice())
					.setScale(2, RoundingMode.HALF_UP);
			request.addLine(
					item,
					lineCommand.quantity(),
					item.getUnit(),
					lineCommand.estimatedUnitPrice(),
					estimatedAmount,
					lineCommand.description()
			);
		}

		return purchaseRequestRepository.save(request).getId();
	}

	public List<PurchaseRequestSummary> findMine(@NotNull Long requesterId) {
		getActiveUser(requesterId);
		requireRole(requesterId, RoleCode.REQUESTER);
		return purchaseRequestRepository.findAllByRequester_IdOrderByCreatedAtDesc(requesterId).stream()
				.map(this::toSummary)
				.toList();
	}

	public PurchaseRequestDetail getMine(@NotNull Long requestId, @NotNull Long requesterId) {
		AppUser requester = getActiveUser(requesterId);
		requireRole(requesterId, RoleCode.REQUESTER);
		PurchaseRequest request = getRequest(requestId);
		validateOwner(request, requester.getId());
		return toDetail(request);
	}

	@Transactional
	public void update(
			@NotNull Long requestId,
			@NotNull Long requesterId,
			@NotNull @Valid UpdatePurchaseRequestCommand command
	) {
		AppUser requester = getActiveUser(requesterId);
		requireRole(requesterId, RoleCode.REQUESTER);
		PurchaseRequest request = getRequest(requestId);
		validateOwner(request, requester.getId());
		request.updateDetails(command.title(), command.purpose(), command.neededDate());
	}

	@Transactional
	public void submit(Long requestId, Long requesterId) {
		AppUser requester = getActiveUser(requesterId);
		requireRole(requesterId, RoleCode.REQUESTER);
		PurchaseRequest request = getRequest(requestId);
		validateOwner(request, requester.getId());
		request.submit(LocalDateTime.now());
	}

	@Transactional
	public void approve(Long requestId, Long approverId) {
		AppUser approver = getActiveUser(approverId);
		requireRole(approverId, RoleCode.BUYER);
		getRequest(requestId).approve(approver, LocalDateTime.now());
	}

	@Transactional
	public void reject(Long requestId, Long rejectorId, String reason) {
		AppUser rejector = getActiveUser(rejectorId);
		requireRole(rejectorId, RoleCode.BUYER);
		getRequest(requestId).reject(rejector, LocalDateTime.now(), reason);
	}

	private PurchaseRequest getRequest(Long requestId) {
		return purchaseRequestRepository.findById(requestId)
				.orElseThrow(() -> new PurchaseRequestNotFoundException(requestId));
	}

	private AppUser getActiveUser(Long userId) {
		return appUserRepository.findById(userId)
				.filter(AppUser::isActive)
				.orElseThrow(() -> new InactivePurchaseUserException(userId));
	}

	private void requireRole(Long userId, RoleCode roleCode) {
		if (!userRoleRepository.existsByAppUser_IdAndRole_Code(userId, roleCode)) {
			throw new PurchaseRoleRequiredException(roleCode);
		}
	}

	private void validateOwner(PurchaseRequest request, Long requesterId) {
		if (!request.getRequester().getId().equals(requesterId)) {
			throw new PurchaseRequestAccessDeniedException(request.getId());
		}
	}

	private PurchaseRequestSummary toSummary(PurchaseRequest request) {
		return new PurchaseRequestSummary(
				request.getId(),
				request.getRequestNumber(),
				request.getTitle(),
				request.getStatus(),
				request.getRequestDate(),
				request.getNeededDate(),
				totalEstimatedAmount(request),
				request.getCreatedAt()
		);
	}

	private PurchaseRequestDetail toDetail(PurchaseRequest request) {
		List<PurchaseRequestLineDetail> lines = request.getLines().stream()
				.map(this::toLineDetail)
				.toList();
		return new PurchaseRequestDetail(
				request.getId(),
				request.getRequestNumber(),
				request.getRequester().getId(),
				request.getRequester().getName(),
				request.getDepartment().getId(),
				request.getDepartment().getCode(),
				request.getDepartment().getName(),
				request.getTitle(),
				request.getPurpose(),
				request.getStatus(),
				request.getRequestDate(),
				request.getNeededDate(),
				request.getSubmittedAt(),
				request.getApprovedAt(),
				request.getRejectionReason(),
				request.getRejectedAt(),
				totalEstimatedAmount(request),
				lines,
				request.getCreatedAt(),
				request.getUpdatedAt()
		);
	}

	private PurchaseRequestLineDetail toLineDetail(PurchaseRequestLine line) {
		return new PurchaseRequestLineDetail(
				line.getId(),
				line.getLineNumber(),
				line.getItem().getId(),
				line.getItem().getCode(),
				line.getItem().getName(),
				line.getQuantity(),
				line.getUnit(),
				line.getEstimatedUnitPrice(),
				line.getEstimatedAmount(),
				line.getDescription()
		);
	}

	private BigDecimal totalEstimatedAmount(PurchaseRequest request) {
		return request.getLines().stream()
				.map(PurchaseRequestLine::getEstimatedAmount)
				.reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
	}
}
