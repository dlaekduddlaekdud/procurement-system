package com.dayoung.procurement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dayoung.procurement.invoice.domain.Invoice;
import com.dayoung.procurement.invoice.domain.InvoiceStatus;
import com.dayoung.procurement.invoice.repository.InvoiceRepository;
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
import com.dayoung.procurement.purchase.domain.PurchaseOrderLine;
import com.dayoung.procurement.purchase.repository.PurchaseOrderLineRepository;
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
class InvoiceApiTest {

	private static final String PASSWORD = "test-password";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private InvoiceRepository invoiceRepository;

	@Autowired
	private PurchaseRequestService purchaseRequestService;

	@Autowired
	private PurchaseOrderService purchaseOrderService;

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
	void createsInvoiceAsBuyer() throws Exception {
		TestOrder testOrder = createSentOrder();
		entityManager.flush();

		mockMvc.perform(post("/api/purchase-orders/{orderId}/invoices", testOrder.orderId())
						.with(httpBasic(testOrder.buyerEmail(), PASSWORD))
						.contentType(MediaType.APPLICATION_JSON)
						.content(createInvoiceJson(testOrder.lineId())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").isNumber());

		Invoice invoice = invoiceRepository.findByVendor_IdAndInvoiceNumber(
				testOrder.vendorId(),
				"INV-API-001"
		).orElseThrow();
		assertEquals(new BigDecimal("999.99"), invoice.getSupplyAmount());
		assertEquals(new BigDecimal("99.00"), invoice.getTaxAmount());
	}

	@Test
	void rejectsInvoiceByRequesterAtApiBoundary() throws Exception {
		AppUser requester = createUser("invoice-api-forbidden@example.com", RoleCode.REQUESTER);
		entityManager.flush();

		mockMvc.perform(post("/api/purchase-orders/{orderId}/invoices", 1L)
						.with(httpBasic(requester.getEmail(), PASSWORD))
						.contentType(MediaType.APPLICATION_JSON)
						.content(createInvoiceJson(1L)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
	}

	@Test
	void cancelsInvoiceAsBuyer() throws Exception {
		TestOrder testOrder = createSentOrder();
		entityManager.flush();
		mockMvc.perform(post("/api/purchase-orders/{orderId}/invoices", testOrder.orderId())
						.with(httpBasic(testOrder.buyerEmail(), PASSWORD))
						.contentType(MediaType.APPLICATION_JSON)
						.content(createInvoiceJson(testOrder.lineId())))
				.andExpect(status().isCreated());
		Invoice invoice = invoiceRepository.findByVendor_IdAndInvoiceNumber(
				testOrder.vendorId(),
				"INV-API-001"
		).orElseThrow();

		mockMvc.perform(post(
						"/api/purchase-orders/{orderId}/invoices/{invoiceId}/cancel",
						testOrder.orderId(),
						invoice.getId()
				)
				.with(httpBasic(testOrder.buyerEmail(), PASSWORD))
				.contentType(MediaType.APPLICATION_JSON)
				.content(cancelJson()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));

		entityManager.flush();
		entityManager.clear();
		assertEquals(InvoiceStatus.CANCELLED,
				invoiceRepository.findById(invoice.getId()).orElseThrow().getStatus());
	}

	private TestOrder createSentOrder() {
		String suffix = String.valueOf(System.nanoTime());
		AppUser requester = createUser("invoice-api-requester-" + suffix + "@example.com", RoleCode.REQUESTER);
		AppUser buyer = createUser("invoice-api-buyer-" + suffix + "@example.com", RoleCode.BUYER);
		Item item = itemRepository.save(new Item(
				"ITEM-INVOICE-API-" + suffix,
				"송장 API 테스트 품목",
				null,
				"EA",
				new BigDecimal("1000.00")
		));
		Vendor vendor = vendorRepository.save(new Vendor(
				"V-INVOICE-" + suffix,
				"송장 API 테스트 공급업체",
				suffix
		));
		Warehouse warehouse = warehouseRepository.findByCode("WH-SEOUL").orElseThrow();
		Long requestId = purchaseRequestService.create(
				requester.getId(),
				new CreatePurchaseRequestCommand(
						"송장 API 테스트 구매요청",
						null,
						LocalDate.now(),
						List.of(new CreatePurchaseRequestLineCommand(
								item.getId(),
								new BigDecimal("3.000"),
								new BigDecimal("333.33"),
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
		return new TestOrder(orderId, line.getId(), vendor.getId(), buyer.getEmail());
	}

	private AppUser createUser(String email, RoleCode roleCode) {
		Department department = departmentRepository.findByCode("IT").orElseThrow();
		AppUser user = appUserRepository.save(new AppUser(
				email,
				passwordEncoder.encode(PASSWORD),
				"송장 API 테스트",
				department
		));
		Role role = roleRepository.findByCode(roleCode).orElseThrow();
		userRoleRepository.save(new UserRole(user, role));
		return user;
	}

	private String createInvoiceJson(Long lineId) {
		return """
				{
				  "invoiceNumber": "INV-API-001",
				  "invoiceDate": "%s",
				  "postingDate": "%s",
				  "lines": [
				    {
				      "purchaseOrderLineId": %d,
				      "quantity": 3.000,
				      "unitPrice": 333.33
				    }
				  ]
				}
				""".formatted(LocalDate.now(), LocalDate.now(), lineId);
	}

	private String cancelJson() {
		return """
				{
				  "postingDate": "%s",
				  "reason": "송장 API 취소 테스트"
				}
				""".formatted(LocalDate.now());
	}

	private record TestOrder(Long orderId, Long lineId, Long vendorId, String buyerEmail) {
	}
}
