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
import com.dayoung.procurement.purchase.application.CreatePurchaseOrderCommand;
import com.dayoung.procurement.purchase.application.CreatePurchaseRequestCommand;
import com.dayoung.procurement.purchase.application.CreatePurchaseRequestLineCommand;
import com.dayoung.procurement.purchase.application.PurchaseOrderService;
import com.dayoung.procurement.purchase.application.PurchaseRequestService;
import com.dayoung.procurement.purchase.domain.PurchaseOrder;
import com.dayoung.procurement.purchase.domain.PurchaseOrderLine;
import com.dayoung.procurement.purchase.domain.PurchaseOrderStatus;
import com.dayoung.procurement.purchase.repository.PurchaseOrderLineRepository;
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
class GoodsReceiptApiTest {

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
	private PurchaseOrderLineRepository purchaseOrderLineRepository;

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
	void createsPartialGoodsReceiptAsBuyer() throws Exception {
		TestOrder testOrder = createSentOrder();
		entityManager.flush();

		mockMvc.perform(post("/api/purchase-orders/{orderId}/goods-receipts", testOrder.orderId())
						.with(httpBasic(testOrder.buyerEmail(), PASSWORD))
						.contentType(MediaType.APPLICATION_JSON)
						.content(createReceiptJson(testOrder.lineId(), "6.000")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").isNumber());

		entityManager.flush();
		entityManager.clear();
		PurchaseOrder order = purchaseOrderRepository.findById(testOrder.orderId()).orElseThrow();
		assertEquals(PurchaseOrderStatus.PARTIALLY_RECEIVED, order.getStatus());
	}

	@Test
	void rejectsGoodsReceiptByRequesterAtApiBoundary() throws Exception {
		AppUser requester = createUser("receipt-api-forbidden@example.com", RoleCode.REQUESTER);
		entityManager.flush();

		mockMvc.perform(post("/api/purchase-orders/{orderId}/goods-receipts", 1L)
						.with(httpBasic(requester.getEmail(), PASSWORD))
						.contentType(MediaType.APPLICATION_JSON)
						.content(createReceiptJson(1L, "1.000")))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
	}

	@Test
	void rejectsQuantityExceedingPurchaseOrderAtApiBoundary() throws Exception {
		TestOrder testOrder = createSentOrder();
		entityManager.flush();

		mockMvc.perform(post("/api/purchase-orders/{orderId}/goods-receipts", testOrder.orderId())
						.with(httpBasic(testOrder.buyerEmail(), PASSWORD))
						.contentType(MediaType.APPLICATION_JSON)
						.content(createReceiptJson(testOrder.lineId(), "11.000")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("PURCHASE_ORDER_QUANTITY_EXCEEDED"));
	}

	private TestOrder createSentOrder() {
		String suffix = String.valueOf(System.nanoTime());
		AppUser requester = createUser("receipt-api-requester-" + suffix + "@example.com", RoleCode.REQUESTER);
		AppUser buyer = createUser("receipt-api-buyer-" + suffix + "@example.com", RoleCode.BUYER);
		Item item = itemRepository.save(new Item(
				"ITEM-RECEIPT-API-" + suffix,
				"입고 API 테스트 품목",
				null,
				"EA",
				new BigDecimal("1000.00")
		));
		Vendor vendor = vendorRepository.save(new Vendor(
				"V-RECEIPT-API-" + suffix,
				"입고 API 테스트 공급업체",
				suffix
		));
		Warehouse warehouse = warehouseRepository.findByCode("WH-SEOUL").orElseThrow();
		Long requestId = purchaseRequestService.create(
				requester.getId(),
				new CreatePurchaseRequestCommand(
						"입고 API 테스트 구매요청",
						null,
						LocalDate.now(),
						List.of(new CreatePurchaseRequestLineCommand(
								item.getId(),
								new BigDecimal("10.000"),
								new BigDecimal("1000.00"),
								null
						))
				)
		);
		purchaseRequestService.submit(requestId, requester.getId());
		purchaseRequestService.approve(requestId, buyer.getId());
		Long orderId = purchaseOrderService.createFromApprovedRequest(
				requestId,
				buyer.getId(),
				new CreatePurchaseOrderCommand(vendor.getId(), warehouse.getId(), LocalDate.now())
		);
		purchaseOrderService.send(orderId, buyer.getId());
		PurchaseOrderLine line = purchaseOrderLineRepository
				.findAllByPurchaseOrder_IdOrderByLineNumber(orderId)
				.getFirst();
		return new TestOrder(orderId, line.getId(), buyer.getEmail());
	}

	private AppUser createUser(String email, RoleCode roleCode) {
		Department department = departmentRepository.findByCode("IT").orElseThrow();
		AppUser user = appUserRepository.save(new AppUser(
				email,
				passwordEncoder.encode(PASSWORD),
				"입고 API 테스트",
				department
		));
		Role role = roleRepository.findByCode(roleCode).orElseThrow();
		userRoleRepository.save(new UserRole(user, role));
		return user;
	}

	private String createReceiptJson(Long lineId, String quantity) {
		return """
				{
				  "postingDate": "%s",
				  "lines": [
				    {
				      "purchaseOrderLineId": %d,
				      "quantity": %s
				    }
				  ]
				}
				""".formatted(LocalDate.now(), lineId, quantity);
	}

	private record TestOrder(Long orderId, Long lineId, String buyerEmail) {
	}
}
