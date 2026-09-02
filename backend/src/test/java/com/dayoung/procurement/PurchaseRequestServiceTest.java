package com.dayoung.procurement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dayoung.procurement.masterdata.domain.Department;
import com.dayoung.procurement.masterdata.domain.Item;
import com.dayoung.procurement.masterdata.repository.DepartmentRepository;
import com.dayoung.procurement.masterdata.repository.ItemRepository;
import com.dayoung.procurement.purchase.application.CreatePurchaseRequestCommand;
import com.dayoung.procurement.purchase.application.CreatePurchaseRequestLineCommand;
import com.dayoung.procurement.purchase.application.PurchaseRequestService;
import com.dayoung.procurement.purchase.domain.PurchaseRequest;
import com.dayoung.procurement.purchase.domain.PurchaseRequestStatus;
import com.dayoung.procurement.purchase.exception.PurchaseRequestAccessDeniedException;
import com.dayoung.procurement.purchase.exception.PurchaseRoleRequiredException;
import com.dayoung.procurement.purchase.exception.SelfApprovalNotAllowedException;
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
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class PurchaseRequestServiceTest {

	@Autowired
	private PurchaseRequestService purchaseRequestService;

	@Autowired
	private PurchaseRequestRepository purchaseRequestRepository;

	@Autowired
	private AppUserRepository appUserRepository;

	@Autowired
	private UserRoleRepository userRoleRepository;

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private DepartmentRepository departmentRepository;

	@Autowired
	private ItemRepository itemRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void createsSubmitsAndApprovesPurchaseRequest() {
		AppUser requester = createUser("service-requester@example.com", RoleCode.REQUESTER);
		AppUser buyer = createUser("service-buyer@example.com", RoleCode.BUYER);
		Item item = createItem();

		Long requestId = purchaseRequestService.create(requester.getId(), createCommand(item.getId()));
		purchaseRequestService.submit(requestId, requester.getId());
		purchaseRequestService.approve(requestId, buyer.getId());
		entityManager.flush();
		entityManager.clear();

		PurchaseRequest savedRequest = purchaseRequestRepository.findById(requestId).orElseThrow();
		assertEquals(PurchaseRequestStatus.APPROVED, savedRequest.getStatus());
		assertEquals(buyer.getId(), savedRequest.getApprovedBy().getId());
		assertEquals(new BigDecimal("7500000.00"), savedRequest.getLines().getFirst().getEstimatedAmount());
	}

	@Test
	void rejectsSubmissionByAnotherRequester() {
		AppUser owner = createUser("service-owner@example.com", RoleCode.REQUESTER);
		AppUser anotherRequester = createUser("service-another@example.com", RoleCode.REQUESTER);
		Item item = createItem();
		Long requestId = purchaseRequestService.create(owner.getId(), createCommand(item.getId()));

		assertThrows(PurchaseRequestAccessDeniedException.class,
				() -> purchaseRequestService.submit(requestId, anotherRequester.getId()));
	}

	@Test
	void rejectsApprovalWithoutBuyerRole() {
		AppUser requester = createUser("service-no-buyer@example.com", RoleCode.REQUESTER);
		Item item = createItem();
		Long requestId = purchaseRequestService.create(requester.getId(), createCommand(item.getId()));
		purchaseRequestService.submit(requestId, requester.getId());

		assertThrows(PurchaseRoleRequiredException.class,
				() -> purchaseRequestService.approve(requestId, requester.getId()));
	}

	@Test
	void rejectsSelfApprovalEvenWithBuyerRole() {
		AppUser requester = createUser("service-self-approve@example.com", RoleCode.REQUESTER, RoleCode.BUYER);
		Item item = createItem();
		Long requestId = purchaseRequestService.create(requester.getId(), createCommand(item.getId()));
		purchaseRequestService.submit(requestId, requester.getId());

		assertThrows(SelfApprovalNotAllowedException.class,
				() -> purchaseRequestService.approve(requestId, requester.getId()));
	}

	@Test
	void rejectsPurchaseRequestWithBuyerAndReason() {
		AppUser requester = createUser("service-reject-requester@example.com", RoleCode.REQUESTER);
		AppUser buyer = createUser("service-reject-buyer@example.com", RoleCode.BUYER);
		Item item = createItem();
		Long requestId = purchaseRequestService.create(requester.getId(), createCommand(item.getId()));
		purchaseRequestService.submit(requestId, requester.getId());

		purchaseRequestService.reject(requestId, buyer.getId(), "예산을 초과했습니다.");
		entityManager.flush();
		entityManager.clear();

		PurchaseRequest savedRequest = purchaseRequestRepository.findById(requestId).orElseThrow();
		assertEquals(PurchaseRequestStatus.REJECTED, savedRequest.getStatus());
		assertEquals(buyer.getId(), savedRequest.getRejectedBy().getId());
		assertEquals("예산을 초과했습니다.", savedRequest.getRejectionReason());
	}

	private AppUser createUser(String email, RoleCode... roleCodes) {
		Department department = departmentRepository.findByCode("IT").orElseThrow();
		AppUser user = appUserRepository.save(new AppUser(email, "encoded-password", "서비스 테스트 사용자", department));
		for (RoleCode roleCode : roleCodes) {
			Role role = roleRepository.findByCode(roleCode).orElseThrow();
			userRoleRepository.save(new UserRole(user, role));
		}
		return user;
	}

	private Item createItem() {
		return itemRepository.save(new Item(
				"ITEM-SERVICE-" + System.nanoTime(),
				"서비스 테스트 품목",
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
}
