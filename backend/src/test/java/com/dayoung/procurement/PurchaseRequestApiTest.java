package com.dayoung.procurement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dayoung.procurement.masterdata.domain.Department;
import com.dayoung.procurement.masterdata.domain.Item;
import com.dayoung.procurement.masterdata.repository.DepartmentRepository;
import com.dayoung.procurement.masterdata.repository.ItemRepository;
import com.dayoung.procurement.purchase.application.CreatePurchaseRequestCommand;
import com.dayoung.procurement.purchase.application.CreatePurchaseRequestLineCommand;
import com.dayoung.procurement.purchase.application.PurchaseRequestService;
import com.dayoung.procurement.purchase.domain.PurchaseRequest;
import com.dayoung.procurement.purchase.domain.PurchaseRequestStatus;
import com.dayoung.procurement.purchase.repository.PurchaseRequestRepository;
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
class PurchaseRequestApiTest {

	private static final String PASSWORD = "test-password";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private PurchaseRequestService purchaseRequestService;

	@Autowired
	private PurchaseRequestRepository purchaseRequestRepository;

	@Autowired
	private DepartmentRepository departmentRepository;

	@Autowired
	private ItemRepository itemRepository;

	@Autowired
	private AppUserRepository appUserRepository;

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private UserRoleRepository userRoleRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void createsPurchaseRequestAsAuthenticatedRequester() throws Exception {
		AppUser requester = createUser("api-create-requester@example.com", RoleCode.REQUESTER);
		Item item = createItem();
		entityManager.flush();

		mockMvc.perform(post("/api/purchase-requests")
						.with(httpBasic(requester.getEmail(), PASSWORD))
						.contentType(MediaType.APPLICATION_JSON)
						.content(createRequestJson(item.getId(), "3.000")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").isNumber());

		PurchaseRequest request = purchaseRequestRepository
				.findAllByRequester_IdOrderByCreatedAtDesc(requester.getId())
				.getFirst();
		assertEquals(PurchaseRequestStatus.DRAFT, request.getStatus());
		assertEquals(new BigDecimal("7500000.00"), request.getLines().getFirst().getEstimatedAmount());
	}

	@Test
	void rejectsPurchaseRequestCreationWithoutAuthentication() throws Exception {
		mockMvc.perform(post("/api/purchase-requests")
						.contentType(MediaType.APPLICATION_JSON)
						.content(createRequestJson(1L, "3.000")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
	}

	@Test
	void findsOnlyPurchaseRequestsCreatedByAuthenticatedRequester() throws Exception {
		AppUser requester = createUser("api-list-requester@example.com", RoleCode.REQUESTER);
		AppUser anotherRequester = createUser("api-list-another@example.com", RoleCode.REQUESTER);
		Item item = createItem();
		Long requestId = purchaseRequestService.create(requester.getId(), createCommand(item.getId()));
		purchaseRequestService.create(anotherRequester.getId(), createCommand(item.getId()));
		entityManager.flush();

		mockMvc.perform(get("/api/purchase-requests")
						.with(httpBasic(requester.getEmail(), PASSWORD)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.length()").value(1))
				.andExpect(jsonPath("$.data[0].id").value(requestId))
				.andExpect(jsonPath("$.data[0].totalEstimatedAmount").value(7500000.00));
	}

	@Test
	void rejectsPurchaseRequestListByBuyerAtApiBoundary() throws Exception {
		AppUser buyer = createUser("api-list-buyer@example.com", RoleCode.BUYER);
		entityManager.flush();

		mockMvc.perform(get("/api/purchase-requests")
						.with(httpBasic(buyer.getEmail(), PASSWORD)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
	}

	@Test
	void findsPurchaseRequestDetailCreatedByAuthenticatedRequester() throws Exception {
		AppUser requester = createUser("api-detail-requester@example.com", RoleCode.REQUESTER);
		Item item = createItem();
		Long requestId = purchaseRequestService.create(requester.getId(), createCommand(item.getId()));
		entityManager.flush();

		mockMvc.perform(get("/api/purchase-requests/{requestId}", requestId)
						.with(httpBasic(requester.getEmail(), PASSWORD)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").value(requestId))
				.andExpect(jsonPath("$.data.requesterId").value(requester.getId()))
				.andExpect(jsonPath("$.data.lines[0].itemId").value(item.getId()))
				.andExpect(jsonPath("$.data.totalEstimatedAmount").value(7500000.00));
	}

	@Test
	void rejectsPurchaseRequestDetailOwnedByAnotherRequester() throws Exception {
		AppUser owner = createUser("api-detail-owner@example.com", RoleCode.REQUESTER);
		AppUser anotherRequester = createUser("api-detail-another@example.com", RoleCode.REQUESTER);
		Item item = createItem();
		Long requestId = purchaseRequestService.create(owner.getId(), createCommand(item.getId()));
		entityManager.flush();

		mockMvc.perform(get("/api/purchase-requests/{requestId}", requestId)
						.with(httpBasic(anotherRequester.getEmail(), PASSWORD)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
	}

	@Test
	void rejectsInvalidPurchaseRequestInput() throws Exception {
		AppUser requester = createUser("api-validation-requester@example.com", RoleCode.REQUESTER);
		Item item = createItem();
		entityManager.flush();

		mockMvc.perform(post("/api/purchase-requests")
						.with(httpBasic(requester.getEmail(), PASSWORD))
						.contentType(MediaType.APPLICATION_JSON)
						.content(createRequestJson(item.getId(), "0")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
	}

	@Test
	void rejectsApprovalByRequesterAtApiBoundary() throws Exception {
		AppUser requester = createUser("api-forbidden-requester@example.com", RoleCode.REQUESTER);
		Item item = createItem();
		Long requestId = createAndSubmit(requester, item);
		entityManager.flush();

		mockMvc.perform(post("/api/purchase-requests/{requestId}/approve", requestId)
						.with(httpBasic(requester.getEmail(), PASSWORD)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
	}

	@Test
	void approvesSubmittedPurchaseRequestAsBuyer() throws Exception {
		AppUser requester = createUser("api-approve-requester@example.com", RoleCode.REQUESTER);
		AppUser buyer = createUser("api-approve-buyer@example.com", RoleCode.BUYER);
		Item item = createItem();
		Long requestId = createAndSubmit(requester, item);
		entityManager.flush();

		mockMvc.perform(post("/api/purchase-requests/{requestId}/approve", requestId)
						.with(httpBasic(buyer.getEmail(), PASSWORD)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));

		entityManager.flush();
		entityManager.clear();
		PurchaseRequest request = purchaseRequestRepository.findById(requestId).orElseThrow();
		assertEquals(PurchaseRequestStatus.APPROVED, request.getStatus());
		assertEquals(buyer.getId(), request.getApprovedBy().getId());
	}

	private Long createAndSubmit(AppUser requester, Item item) {
		Long requestId = purchaseRequestService.create(requester.getId(), createCommand(item.getId()));
		purchaseRequestService.submit(requestId, requester.getId());
		return requestId;
	}

	private AppUser createUser(String email, RoleCode roleCode) {
		Department department = departmentRepository.findByCode("IT").orElseThrow();
		AppUser user = appUserRepository.save(new AppUser(
				email,
				passwordEncoder.encode(PASSWORD),
				"API 테스트 사용자",
				department
		));
		Role role = roleRepository.findByCode(roleCode).orElseThrow();
		userRoleRepository.save(new UserRole(user, role));
		return user;
	}

	private Item createItem() {
		return itemRepository.save(new Item(
				"ITEM-API-" + System.nanoTime(),
				"API 테스트 품목",
				null,
				"EA",
				new BigDecimal("2500000.00")
		));
	}

	private CreatePurchaseRequestCommand createCommand(Long itemId) {
		return new CreatePurchaseRequestCommand(
				"개발 장비 구매",
				"신규 입사자 장비 지급",
				LocalDate.now().plusDays(14),
				List.of(new CreatePurchaseRequestLineCommand(
						itemId,
						new BigDecimal("3.000"),
						new BigDecimal("2500000.00"),
						"개발자용 노트북"
				))
		);
	}

	private String createRequestJson(Long itemId, String quantity) {
		return """
				{
				  "title": "개발 장비 구매",
				  "purpose": "신규 입사자 장비 지급",
				  "neededDate": "%s",
				  "lines": [
				    {
				      "itemId": %d,
				      "quantity": %s,
				      "estimatedUnitPrice": 2500000.00,
				      "description": "개발자용 노트북"
				    }
				  ]
				}
				""".formatted(LocalDate.now().plusDays(14), itemId, quantity);
	}
}
