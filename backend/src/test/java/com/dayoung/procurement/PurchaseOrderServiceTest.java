package com.dayoung.procurement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dayoung.procurement.masterdata.domain.Department;
import com.dayoung.procurement.masterdata.domain.Item;
import com.dayoung.procurement.masterdata.domain.Vendor;
import com.dayoung.procurement.masterdata.domain.Warehouse;
import com.dayoung.procurement.masterdata.repository.DepartmentRepository;
import com.dayoung.procurement.masterdata.repository.ItemRepository;
import com.dayoung.procurement.masterdata.repository.VendorRepository;
import com.dayoung.procurement.masterdata.repository.WarehouseRepository;
import com.dayoung.procurement.purchase.application.CreatePurchaseOrderCommand;
import com.dayoung.procurement.purchase.application.CreatePurchaseRequestCommand;
import com.dayoung.procurement.purchase.application.CreatePurchaseRequestLineCommand;
import com.dayoung.procurement.purchase.application.PurchaseOrderService;
import com.dayoung.procurement.purchase.application.PurchaseRequestService;
import com.dayoung.procurement.purchase.domain.PurchaseOrder;
import com.dayoung.procurement.purchase.domain.PurchaseOrderStatus;
import com.dayoung.procurement.purchase.exception.InvalidPurchaseRequestStateException;
import com.dayoung.procurement.purchase.exception.InvalidPurchaseOrderStateException;
import com.dayoung.procurement.purchase.exception.PurchaseOrderAccessDeniedException;
import com.dayoung.procurement.purchase.exception.PurchaseOrderAlreadyExistsException;
import com.dayoung.procurement.purchase.repository.PurchaseOrderRepository;
import com.dayoung.procurement.user.domain.AppUser;
import com.dayoung.procurement.user.domain.Role;
import com.dayoung.procurement.user.domain.RoleCode;
import com.dayoung.procurement.user.domain.UserRole;
import com.dayoung.procurement.user.repository.AppUserRepository;
import com.dayoung.procurement.user.repository.RoleRepository;
import com.dayoung.procurement.user.repository.UserRoleRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class PurchaseOrderServiceTest {

	@Autowired
	private PurchaseOrderService purchaseOrderService;

	@Autowired
	private PurchaseRequestService purchaseRequestService;

	@Autowired
	private PurchaseOrderRepository purchaseOrderRepository;

	@Autowired
	private DepartmentRepository departmentRepository;

	@Autowired
	private ItemRepository itemRepository;

	@Autowired
	private VendorRepository vendorRepository;

	@Autowired
	private WarehouseRepository warehouseRepository;

	@Autowired
	private AppUserRepository appUserRepository;

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private UserRoleRepository userRoleRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void createsPurchaseOrderFromApprovedRequest() {
		AppUser requester = createUser("order-service-requester@example.com", RoleCode.REQUESTER);
		AppUser buyer = createUser("order-service-buyer@example.com", RoleCode.BUYER);
		Item item = createItem();
		Vendor vendor = createVendor();
		Warehouse warehouse = warehouseRepository.findByCode("WH-SEOUL").orElseThrow();
		Long requestId = createApprovedRequest(requester, buyer, item);
		LocalDate deliveryDate = LocalDate.now().plusDays(14);

		Long orderId = purchaseOrderService.createFromApprovedRequest(
				requestId,
				buyer.getId(),
				new CreatePurchaseOrderCommand(vendor.getId(), warehouse.getId(), deliveryDate)
		);
		entityManager.flush();
		entityManager.clear();

		PurchaseOrder order = purchaseOrderRepository.findById(orderId).orElseThrow();
		assertEquals(PurchaseOrderStatus.CREATED, order.getStatus());
		assertEquals(requestId, order.getPurchaseRequest().getId());
		assertEquals(buyer.getId(), order.getBuyer().getId());
		assertEquals(vendor.getId(), order.getVendor().getId());
		assertEquals(warehouse.getId(), order.getWarehouse().getId());
		assertEquals(1, order.getLines().size());
		assertEquals(item.getId(), order.getLines().getFirst().getItem().getId());
		assertEquals(new BigDecimal("7500000.00"), order.getLines().getFirst().getSupplyAmount());
		assertEquals(new BigDecimal("750000.00"), order.getLines().getFirst().getTaxAmount());
		assertEquals(new BigDecimal("8250000.00"), order.getLines().getFirst().getTotalAmount());
	}

	@Test
	void rejectsPurchaseOrderFromUnapprovedRequest() {
		AppUser requester = createUser("order-service-draft-requester@example.com", RoleCode.REQUESTER);
		AppUser buyer = createUser("order-service-draft-buyer@example.com", RoleCode.BUYER);
		Item item = createItem();
		Vendor vendor = createVendor();
		Warehouse warehouse = warehouseRepository.findByCode("WH-SEOUL").orElseThrow();
		Long requestId = purchaseRequestService.create(requester.getId(), createCommand(item.getId()));

		assertThrows(InvalidPurchaseRequestStateException.class, () ->
				purchaseOrderService.createFromApprovedRequest(
						requestId,
						buyer.getId(),
						new CreatePurchaseOrderCommand(vendor.getId(), warehouse.getId(), LocalDate.now().plusDays(14))
				)
		);
	}

	@Test
	void rejectsDuplicatePurchaseOrderForSameRequest() {
		AppUser requester = createUser("order-service-duplicate-requester@example.com", RoleCode.REQUESTER);
		AppUser buyer = createUser("order-service-duplicate-buyer@example.com", RoleCode.BUYER);
		Item item = createItem();
		Vendor vendor = createVendor();
		Warehouse warehouse = warehouseRepository.findByCode("WH-SEOUL").orElseThrow();
		Long requestId = createApprovedRequest(requester, buyer, item);
		CreatePurchaseOrderCommand command = new CreatePurchaseOrderCommand(
				vendor.getId(),
				warehouse.getId(),
				LocalDate.now().plusDays(14)
		);
		purchaseOrderService.createFromApprovedRequest(requestId, buyer.getId(), command);

		assertThrows(PurchaseOrderAlreadyExistsException.class,
				() -> purchaseOrderService.createFromApprovedRequest(requestId, buyer.getId(), command));
	}

	@Test
	void sendsCreatedPurchaseOrder() {
		AppUser requester = createUser("order-send-requester@example.com", RoleCode.REQUESTER);
		AppUser buyer = createUser("order-send-buyer@example.com", RoleCode.BUYER);
		Long orderId = createPurchaseOrder(requester, buyer);

		purchaseOrderService.send(orderId, buyer.getId());
		entityManager.flush();
		entityManager.clear();

		PurchaseOrder order = purchaseOrderRepository.findById(orderId).orElseThrow();
		assertEquals(PurchaseOrderStatus.SENT, order.getStatus());
	}

	@Test
	void rejectsSendingPurchaseOrderTwice() {
		AppUser requester = createUser("order-resend-requester@example.com", RoleCode.REQUESTER);
		AppUser buyer = createUser("order-resend-buyer@example.com", RoleCode.BUYER);
		Long orderId = createPurchaseOrder(requester, buyer);
		purchaseOrderService.send(orderId, buyer.getId());

		assertThrows(InvalidPurchaseOrderStateException.class,
				() -> purchaseOrderService.send(orderId, buyer.getId()));
	}

	@Test
	void rejectsSendingPurchaseOrderByDifferentBuyer() {
		AppUser requester = createUser("order-owner-requester@example.com", RoleCode.REQUESTER);
		AppUser buyer = createUser("order-owner-buyer@example.com", RoleCode.BUYER);
		AppUser otherBuyer = createUser("order-other-buyer@example.com", RoleCode.BUYER);
		Long orderId = createPurchaseOrder(requester, buyer);

		assertThrows(PurchaseOrderAccessDeniedException.class,
				() -> purchaseOrderService.send(orderId, otherBuyer.getId()));
	}

	private Long createPurchaseOrder(AppUser requester, AppUser buyer) {
		Item item = createItem();
		Vendor vendor = createVendor();
		Warehouse warehouse = warehouseRepository.findByCode("WH-SEOUL").orElseThrow();
		Long requestId = createApprovedRequest(requester, buyer, item);
		return purchaseOrderService.createFromApprovedRequest(
				requestId,
				buyer.getId(),
				new CreatePurchaseOrderCommand(vendor.getId(), warehouse.getId(), LocalDate.now().plusDays(14))
		);
	}

	private Long createApprovedRequest(AppUser requester, AppUser buyer, Item item) {
		Long requestId = purchaseRequestService.create(requester.getId(), createCommand(item.getId()));
		purchaseRequestService.submit(requestId, requester.getId());
		purchaseRequestService.approve(requestId, buyer.getId());
		return requestId;
	}

	private AppUser createUser(String email, RoleCode roleCode) {
		Department department = departmentRepository.findByCode("IT").orElseThrow();
		AppUser user = appUserRepository.save(new AppUser(email, "encoded-password", "발주 서비스 테스트", department));
		Role role = roleRepository.findByCode(roleCode).orElseThrow();
		userRoleRepository.save(new UserRole(user, role));
		return user;
	}

	private Item createItem() {
		return itemRepository.save(new Item(
				"ITEM-ORDER-SERVICE-" + System.nanoTime(),
				"발주 서비스 테스트 품목",
				null,
				"EA",
				new BigDecimal("2500000.00")
		));
	}

	private Vendor createVendor() {
		String suffix = String.valueOf(System.nanoTime());
		return vendorRepository.save(new Vendor("V-" + suffix, "발주 서비스 공급업체", suffix));
	}

	private CreatePurchaseRequestCommand createCommand(Long itemId) {
		return new CreatePurchaseRequestCommand(
				"발주 전환 요청",
				"발주 전환 테스트",
				LocalDate.now().plusDays(14),
				List.of(new CreatePurchaseRequestLineCommand(
						itemId,
						new BigDecimal("3.000"),
						new BigDecimal("2500000.00"),
						"테스트 품목"
				))
		);
	}
}
