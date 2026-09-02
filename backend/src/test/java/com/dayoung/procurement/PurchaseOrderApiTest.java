package com.dayoung.procurement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dayoung.procurement.masterdata.domain.Department;
import com.dayoung.procurement.masterdata.domain.Item;
import com.dayoung.procurement.masterdata.domain.Vendor;
import com.dayoung.procurement.masterdata.domain.Warehouse;
import com.dayoung.procurement.masterdata.repository.DepartmentRepository;
import com.dayoung.procurement.masterdata.repository.ItemRepository;
import com.dayoung.procurement.masterdata.repository.VendorRepository;
import com.dayoung.procurement.masterdata.repository.WarehouseRepository;
import com.dayoung.procurement.purchase.application.CreatePurchaseRequestCommand;
import com.dayoung.procurement.purchase.application.CreatePurchaseRequestLineCommand;
import com.dayoung.procurement.purchase.application.CreatePurchaseOrderCommand;
import com.dayoung.procurement.purchase.application.PurchaseOrderService;
import com.dayoung.procurement.purchase.application.PurchaseRequestService;
import com.dayoung.procurement.purchase.domain.PurchaseOrder;
import com.dayoung.procurement.purchase.domain.PurchaseOrderStatus;
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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@SpringBootTest
@Transactional
class PurchaseOrderApiTest {

	private static final String PASSWORD = "test-password";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private PurchaseRequestService purchaseRequestService;

	@Autowired
	private PurchaseOrderService purchaseOrderService;

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
	void createsPurchaseOrderFromApprovedRequestAsBuyer() throws Exception {
		AppUser requester = createUser("order-api-requester@example.com", RoleCode.REQUESTER);
		AppUser buyer = createUser("order-api-buyer@example.com", RoleCode.BUYER);
		Item item = createItem();
		Vendor vendor = createVendor();
		Warehouse warehouse = warehouseRepository.findByCode("WH-SEOUL").orElseThrow();
		Long requestId = createApprovedRequest(requester, buyer, item);
		entityManager.flush();

		mockMvc.perform(post("/api/purchase-requests/{requestId}/purchase-order", requestId)
						.with(httpBasic(buyer.getEmail(), PASSWORD))
						.contentType(MediaType.APPLICATION_JSON)
						.content(createOrderJson(vendor.getId(), warehouse.getId())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").isNumber());

		entityManager.flush();
		entityManager.clear();
		PurchaseOrder order = purchaseOrderRepository.findByPurchaseRequest_Id(requestId).orElseThrow();
		assertEquals(1, order.getLines().size());
		assertEquals(requestId, order.getLines().getFirst().getPurchaseRequestLine().getPurchaseRequest().getId());
	}

	@Test
	void rejectsPurchaseOrderCreationByRequesterAtApiBoundary() throws Exception {
		AppUser requester = createUser("order-api-forbidden-requester@example.com", RoleCode.REQUESTER);
		Vendor vendor = createVendor();
		Warehouse warehouse = warehouseRepository.findByCode("WH-SEOUL").orElseThrow();
		entityManager.flush();

		mockMvc.perform(post("/api/purchase-requests/{requestId}/purchase-order", 1L)
						.with(httpBasic(requester.getEmail(), PASSWORD))
						.contentType(MediaType.APPLICATION_JSON)
						.content(createOrderJson(vendor.getId(), warehouse.getId())))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
	}

	@Test
	void rejectsPurchaseOrderFromUnapprovedRequestAtApiBoundary() throws Exception {
		AppUser requester = createUser("order-api-draft-requester@example.com", RoleCode.REQUESTER);
		AppUser buyer = createUser("order-api-draft-buyer@example.com", RoleCode.BUYER);
		Item item = createItem();
		Vendor vendor = createVendor();
		Warehouse warehouse = warehouseRepository.findByCode("WH-SEOUL").orElseThrow();
		Long requestId = purchaseRequestService.create(requester.getId(), createCommand(item.getId()));
		entityManager.flush();

		mockMvc.perform(post("/api/purchase-requests/{requestId}/purchase-order", requestId)
						.with(httpBasic(buyer.getEmail(), PASSWORD))
						.contentType(MediaType.APPLICATION_JSON)
						.content(createOrderJson(vendor.getId(), warehouse.getId())))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("INVALID_PURCHASE_REQUEST_STATE"));
	}

	@Test
	void sendsCreatedPurchaseOrderAsBuyer() throws Exception {
		AppUser requester = createUser("order-send-api-requester@example.com", RoleCode.REQUESTER);
		AppUser buyer = createUser("order-send-api-buyer@example.com", RoleCode.BUYER);
		Long orderId = createPurchaseOrder(requester, buyer);
		entityManager.flush();

		mockMvc.perform(post("/api/purchase-orders/{orderId}/send", orderId)
						.with(httpBasic(buyer.getEmail(), PASSWORD)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));

		entityManager.flush();
		entityManager.clear();
		PurchaseOrder order = purchaseOrderRepository.findById(orderId).orElseThrow();
		assertEquals(PurchaseOrderStatus.SENT, order.getStatus());
	}

	@Test
	void rejectsSendingPurchaseOrderByRequesterAtApiBoundary() throws Exception {
		AppUser requester = createUser("order-send-api-forbidden@example.com", RoleCode.REQUESTER);
		entityManager.flush();

		mockMvc.perform(post("/api/purchase-orders/{orderId}/send", 1L)
						.with(httpBasic(requester.getEmail(), PASSWORD)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
	}

	@Test
	void rejectsSendingPurchaseOrderByDifferentBuyerAtApiBoundary() throws Exception {
		AppUser requester = createUser("order-send-owner-requester@example.com", RoleCode.REQUESTER);
		AppUser buyer = createUser("order-send-owner-buyer@example.com", RoleCode.BUYER);
		AppUser otherBuyer = createUser("order-send-other-buyer@example.com", RoleCode.BUYER);
		Long orderId = createPurchaseOrder(requester, buyer);
		entityManager.flush();

		mockMvc.perform(post("/api/purchase-orders/{orderId}/send", orderId)
						.with(httpBasic(otherBuyer.getEmail(), PASSWORD)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
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
		AppUser user = appUserRepository.save(new AppUser(
				email,
				passwordEncoder.encode(PASSWORD),
				"발주 API 테스트",
				department
		));
		Role role = roleRepository.findByCode(roleCode).orElseThrow();
		userRoleRepository.save(new UserRole(user, role));
		return user;
	}

	private Item createItem() {
		return itemRepository.save(new Item(
				"ITEM-ORDER-API-" + System.nanoTime(),
				"발주 API 테스트 품목",
				null,
				"EA",
				new BigDecimal("2500000.00")
		));
	}

	private Vendor createVendor() {
		String suffix = String.valueOf(System.nanoTime());
		return vendorRepository.save(new Vendor("V-" + suffix, "발주 API 공급업체", suffix));
	}

	private CreatePurchaseRequestCommand createCommand(Long itemId) {
		return new CreatePurchaseRequestCommand(
				"발주 전환 요청",
				"발주 전환 API 테스트",
				LocalDate.now().plusDays(14),
				List.of(new CreatePurchaseRequestLineCommand(
						itemId,
						new BigDecimal("3.000"),
						new BigDecimal("2500000.00"),
						"테스트 품목"
				))
		);
	}

	private String createOrderJson(Long vendorId, Long warehouseId) {
		return """
				{
				  "vendorId": %d,
				  "warehouseId": %d,
				  "expectedDeliveryDate": "%s"
				}
				""".formatted(vendorId, warehouseId, LocalDate.now().plusDays(14));
	}
}
