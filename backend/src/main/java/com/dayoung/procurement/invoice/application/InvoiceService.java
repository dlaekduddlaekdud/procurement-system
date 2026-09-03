package com.dayoung.procurement.invoice.application;

import com.dayoung.procurement.closing.application.ClosePeriodService;
import com.dayoung.procurement.invoice.domain.Invoice;
import com.dayoung.procurement.invoice.exception.DuplicateInvoiceException;
import com.dayoung.procurement.invoice.repository.InvoiceRepository;
import com.dayoung.procurement.matching.application.ThreeWayMatchingService;
import com.dayoung.procurement.purchase.domain.PurchaseOrder;
import com.dayoung.procurement.purchase.domain.PurchaseOrderLine;
import com.dayoung.procurement.purchase.exception.InactivePurchaseUserException;
import com.dayoung.procurement.purchase.exception.PurchaseOrderAccessDeniedException;
import com.dayoung.procurement.purchase.exception.PurchaseOrderLineNotFoundException;
import com.dayoung.procurement.purchase.exception.PurchaseOrderNotFoundException;
import com.dayoung.procurement.purchase.exception.PurchaseRoleRequiredException;
import com.dayoung.procurement.purchase.repository.PurchaseOrderRepository;
import com.dayoung.procurement.user.domain.AppUser;
import com.dayoung.procurement.user.domain.RoleCode;
import com.dayoung.procurement.user.repository.AppUserRepository;
import com.dayoung.procurement.user.repository.UserRoleRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@Transactional(readOnly = true)
public class InvoiceService {

	private final InvoiceRepository invoiceRepository;
	private final PurchaseOrderRepository purchaseOrderRepository;
	private final AppUserRepository appUserRepository;
	private final UserRoleRepository userRoleRepository;
	private final ClosePeriodService closePeriodService;
	private final ThreeWayMatchingService matchingService;

	public InvoiceService(
			InvoiceRepository invoiceRepository,
			PurchaseOrderRepository purchaseOrderRepository,
			AppUserRepository appUserRepository,
			UserRoleRepository userRoleRepository,
			ClosePeriodService closePeriodService,
			ThreeWayMatchingService matchingService
	) {
		this.invoiceRepository = invoiceRepository;
		this.purchaseOrderRepository = purchaseOrderRepository;
		this.appUserRepository = appUserRepository;
		this.userRoleRepository = userRoleRepository;
		this.closePeriodService = closePeriodService;
		this.matchingService = matchingService;
	}

	@Transactional
	public Long create(
			@NotNull Long orderId,
			@NotNull Long buyerId,
			@NotNull @Valid CreateInvoiceCommand command
	) {
		AppUser buyer = getActiveBuyer(buyerId);
		closePeriodService.requireOpen(command.postingDate());
		PurchaseOrder order = purchaseOrderRepository.findById(orderId)
				.orElseThrow(() -> new PurchaseOrderNotFoundException(orderId));
		validateOrderAccess(order, buyer);
		order.requireInvoiceAllowed();
		if (invoiceRepository.existsByVendor_IdAndInvoiceNumber(order.getVendor().getId(), command.invoiceNumber())) {
			throw new DuplicateInvoiceException(order.getVendor().getId(), command.invoiceNumber());
		}

		Map<Long, PurchaseOrderLine> orderLines = order.getLines().stream()
				.collect(Collectors.toMap(PurchaseOrderLine::getId, Function.identity()));
		validateUniqueLines(command.lines());
		Invoice invoice = new Invoice(
				command.invoiceNumber(),
				order,
				order.getVendor(),
				buyer,
				command.invoiceDate(),
				command.postingDate(),
				order.getCurrency()
		);

		for (CreateInvoiceLineCommand lineCommand : command.lines()) {
			PurchaseOrderLine orderLine = orderLines.get(lineCommand.purchaseOrderLineId());
			if (orderLine == null) {
				throw new PurchaseOrderLineNotFoundException(orderId, lineCommand.purchaseOrderLineId());
			}
			invoice.addLine(orderLine, lineCommand.quantity(), lineCommand.unitPrice());
		}
		Invoice savedInvoice = invoiceRepository.saveAndFlush(invoice);
		savedInvoice.getLines().forEach(line -> matchingService.matchAndSettle(
				line.getPurchaseOrderLine().getId(),
				savedInvoice.getPostingDate(),
				buyer
		));
		return savedInvoice.getId();
	}

	private void validateUniqueLines(Iterable<CreateInvoiceLineCommand> lines) {
		Set<Long> lineIds = new HashSet<>();
		for (CreateInvoiceLineCommand line : lines) {
			if (!lineIds.add(line.purchaseOrderLineId())) {
				throw new IllegalArgumentException("동일한 발주 라인은 한 송장에 한 번만 포함할 수 있습니다.");
			}
		}
	}

	private void validateOrderAccess(PurchaseOrder order, AppUser buyer) {
		if (!order.getBuyer().getId().equals(buyer.getId())) {
			throw new PurchaseOrderAccessDeniedException(order.getId());
		}
	}

	private AppUser getActiveBuyer(Long buyerId) {
		AppUser buyer = appUserRepository.findById(buyerId)
				.filter(AppUser::isActive)
				.orElseThrow(() -> new InactivePurchaseUserException(buyerId));
		if (!userRoleRepository.existsByAppUser_IdAndRole_Code(buyerId, RoleCode.BUYER)) {
			throw new PurchaseRoleRequiredException(RoleCode.BUYER);
		}
		return buyer;
	}
}
