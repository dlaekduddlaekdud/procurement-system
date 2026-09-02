package com.dayoung.procurement.receipt.application;

import com.dayoung.procurement.purchase.domain.PurchaseOrder;
import com.dayoung.procurement.purchase.domain.PurchaseOrderLine;
import com.dayoung.procurement.purchase.exception.InactivePurchaseUserException;
import com.dayoung.procurement.purchase.exception.PurchaseOrderAccessDeniedException;
import com.dayoung.procurement.purchase.exception.PurchaseOrderLineNotFoundException;
import com.dayoung.procurement.purchase.exception.PurchaseOrderNotFoundException;
import com.dayoung.procurement.purchase.exception.PurchaseRoleRequiredException;
import com.dayoung.procurement.purchase.repository.PurchaseOrderRepository;
import com.dayoung.procurement.receipt.domain.GoodsReceipt;
import com.dayoung.procurement.receipt.domain.GoodsReceiptStatus;
import com.dayoung.procurement.receipt.exception.PurchaseOrderQuantityExceededException;
import com.dayoung.procurement.receipt.repository.GoodsReceiptLineRepository;
import com.dayoung.procurement.receipt.repository.GoodsReceiptRepository;
import com.dayoung.procurement.user.domain.AppUser;
import com.dayoung.procurement.user.domain.RoleCode;
import com.dayoung.procurement.user.repository.AppUserRepository;
import com.dayoung.procurement.user.repository.UserRoleRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
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
public class GoodsReceiptService {

	private final PurchaseOrderRepository purchaseOrderRepository;
	private final GoodsReceiptRepository goodsReceiptRepository;
	private final GoodsReceiptLineRepository goodsReceiptLineRepository;
	private final AppUserRepository appUserRepository;
	private final UserRoleRepository userRoleRepository;
	private final GoodsReceiptNumberGenerator numberGenerator;

	public GoodsReceiptService(
			PurchaseOrderRepository purchaseOrderRepository,
			GoodsReceiptRepository goodsReceiptRepository,
			GoodsReceiptLineRepository goodsReceiptLineRepository,
			AppUserRepository appUserRepository,
			UserRoleRepository userRoleRepository,
			GoodsReceiptNumberGenerator numberGenerator
	) {
		this.purchaseOrderRepository = purchaseOrderRepository;
		this.goodsReceiptRepository = goodsReceiptRepository;
		this.goodsReceiptLineRepository = goodsReceiptLineRepository;
		this.appUserRepository = appUserRepository;
		this.userRoleRepository = userRoleRepository;
		this.numberGenerator = numberGenerator;
	}

	@Transactional
	public Long create(
			@NotNull Long orderId,
			@NotNull Long buyerId,
			@NotNull @Valid CreateGoodsReceiptCommand command
	) {
		AppUser buyer = getActiveBuyer(buyerId);
		PurchaseOrder order = purchaseOrderRepository.findByIdForReceipt(orderId)
				.orElseThrow(() -> new PurchaseOrderNotFoundException(orderId));
		validateOrderAccess(order, buyer);
		order.requireReceivable();

		Map<Long, PurchaseOrderLine> orderLines = order.getLines().stream()
				.collect(Collectors.toMap(PurchaseOrderLine::getId, Function.identity()));
		validateUniqueLines(command.lines());

		GoodsReceipt receipt = new GoodsReceipt(
				numberGenerator.generate(command.postingDate()),
				order,
				order.getWarehouse(),
				buyer,
				command.postingDate(),
				LocalDateTime.now()
		);
		Map<Long, BigDecimal> receivedQuantities = new HashMap<>();

		for (CreateGoodsReceiptLineCommand lineCommand : command.lines()) {
			PurchaseOrderLine orderLine = orderLines.get(lineCommand.purchaseOrderLineId());
			if (orderLine == null) {
				throw new PurchaseOrderLineNotFoundException(orderId, lineCommand.purchaseOrderLineId());
			}
			BigDecimal currentQuantity = getPostedQuantity(orderLine.getId());
			BigDecimal cumulativeQuantity = currentQuantity.add(lineCommand.quantity());
			if (cumulativeQuantity.compareTo(orderLine.getQuantity()) > 0) {
				throw new PurchaseOrderQuantityExceededException(
						orderLine.getId(),
						orderLine.getQuantity(),
						cumulativeQuantity
				);
			}
			receipt.addLine(orderLine, lineCommand.quantity());
			receivedQuantities.put(orderLine.getId(), cumulativeQuantity);
		}

		boolean fullyReceived = order.getLines().stream().allMatch(orderLine ->
				receivedQuantities.getOrDefault(orderLine.getId(), getPostedQuantity(orderLine.getId()))
						.compareTo(orderLine.getQuantity()) == 0
		);
		order.applyReceiptStatus(fullyReceived);
		return goodsReceiptRepository.save(receipt).getId();
	}

	private void validateUniqueLines(Iterable<CreateGoodsReceiptLineCommand> lines) {
		Set<Long> lineIds = new HashSet<>();
		for (CreateGoodsReceiptLineCommand line : lines) {
			if (!lineIds.add(line.purchaseOrderLineId())) {
				throw new IllegalArgumentException("동일한 발주 라인은 한 입고 문서에 한 번만 포함할 수 있습니다.");
			}
		}
	}

	private BigDecimal getPostedQuantity(Long orderLineId) {
		return goodsReceiptLineRepository.sumQuantityByPurchaseOrderLineIdAndStatus(
				orderLineId,
				GoodsReceiptStatus.POSTED
		);
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
