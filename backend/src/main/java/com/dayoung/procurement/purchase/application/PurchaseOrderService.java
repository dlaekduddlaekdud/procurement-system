package com.dayoung.procurement.purchase.application;

import com.dayoung.procurement.masterdata.domain.Vendor;
import com.dayoung.procurement.masterdata.domain.Warehouse;
import com.dayoung.procurement.masterdata.repository.VendorRepository;
import com.dayoung.procurement.masterdata.repository.WarehouseRepository;
import com.dayoung.procurement.purchase.domain.PurchaseOrder;
import com.dayoung.procurement.purchase.domain.PurchaseRequest;
import com.dayoung.procurement.purchase.domain.PurchaseRequestLine;
import com.dayoung.procurement.purchase.exception.InactivePurchaseUserException;
import com.dayoung.procurement.purchase.exception.PurchaseOrderAlreadyExistsException;
import com.dayoung.procurement.purchase.exception.PurchaseRequestNotFoundException;
import com.dayoung.procurement.purchase.exception.PurchaseRoleRequiredException;
import com.dayoung.procurement.purchase.exception.PurchaseVendorNotFoundException;
import com.dayoung.procurement.purchase.exception.PurchaseWarehouseNotFoundException;
import com.dayoung.procurement.purchase.repository.PurchaseOrderRepository;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@Transactional(readOnly = true)
public class PurchaseOrderService {

	private static final BigDecimal VAT_RATE = new BigDecimal("0.10");

	private final PurchaseOrderRepository purchaseOrderRepository;
	private final PurchaseRequestRepository purchaseRequestRepository;
	private final VendorRepository vendorRepository;
	private final WarehouseRepository warehouseRepository;
	private final AppUserRepository appUserRepository;
	private final UserRoleRepository userRoleRepository;
	private final PurchaseOrderNumberGenerator numberGenerator;

	public PurchaseOrderService(
			PurchaseOrderRepository purchaseOrderRepository,
			PurchaseRequestRepository purchaseRequestRepository,
			VendorRepository vendorRepository,
			WarehouseRepository warehouseRepository,
			AppUserRepository appUserRepository,
			UserRoleRepository userRoleRepository,
			PurchaseOrderNumberGenerator numberGenerator
	) {
		this.purchaseOrderRepository = purchaseOrderRepository;
		this.purchaseRequestRepository = purchaseRequestRepository;
		this.vendorRepository = vendorRepository;
		this.warehouseRepository = warehouseRepository;
		this.appUserRepository = appUserRepository;
		this.userRoleRepository = userRoleRepository;
		this.numberGenerator = numberGenerator;
	}

	@Transactional
	public Long createFromApprovedRequest(
			@NotNull Long requestId,
			@NotNull Long buyerId,
			@NotNull @Valid CreatePurchaseOrderCommand command
	) {
		AppUser buyer = getActiveBuyer(buyerId);
		PurchaseRequest request = purchaseRequestRepository.findById(requestId)
				.orElseThrow(() -> new PurchaseRequestNotFoundException(requestId));
		request.requireApprovedForOrder();
		if (purchaseOrderRepository.existsByPurchaseRequest_Id(requestId)) {
			throw new PurchaseOrderAlreadyExistsException(requestId);
		}

		Vendor vendor = vendorRepository.findById(command.vendorId())
				.filter(Vendor::isActive)
				.orElseThrow(() -> new PurchaseVendorNotFoundException(command.vendorId()));
		Warehouse warehouse = warehouseRepository.findById(command.warehouseId())
				.filter(Warehouse::isActive)
				.orElseThrow(() -> new PurchaseWarehouseNotFoundException(command.warehouseId()));
		LocalDate orderDate = LocalDate.now();
		PurchaseOrder order = new PurchaseOrder(
				numberGenerator.generate(orderDate),
				request,
				vendor,
				buyer,
				warehouse,
				orderDate,
				command.expectedDeliveryDate()
		);

		for (PurchaseRequestLine requestLine : request.getLines()) {
			BigDecimal supplyAmount = requestLine.getQuantity()
					.multiply(requestLine.getEstimatedUnitPrice())
					.setScale(2, RoundingMode.HALF_UP);
			BigDecimal taxAmount = supplyAmount.multiply(VAT_RATE).setScale(2, RoundingMode.HALF_UP);
			order.addLine(
					requestLine,
					requestLine.getItem(),
					requestLine.getQuantity(),
					requestLine.getUnit(),
					requestLine.getEstimatedUnitPrice(),
					supplyAmount,
					taxAmount,
					supplyAmount.add(taxAmount),
					command.expectedDeliveryDate()
			);
		}

		return purchaseOrderRepository.save(order).getId();
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
