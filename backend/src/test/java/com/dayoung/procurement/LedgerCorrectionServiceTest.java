package com.dayoung.procurement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dayoung.procurement.audit.domain.AuditEventType;
import com.dayoung.procurement.audit.domain.AuditTargetType;
import com.dayoung.procurement.audit.repository.AuditLogRepository;
import com.dayoung.procurement.closing.domain.ClosePeriod;
import com.dayoung.procurement.closing.exception.OpenPeriodReversalNotAllowedException;
import com.dayoung.procurement.closing.repository.ClosePeriodRepository;
import com.dayoung.procurement.ledger.application.LedgerCorrectionService;
import com.dayoung.procurement.ledger.application.RepostAccrualEntryCommand;
import com.dayoung.procurement.ledger.application.ReverseAccrualEntryCommand;
import com.dayoung.procurement.ledger.domain.AccrualEntry;
import com.dayoung.procurement.ledger.domain.AccrualEntryType;
import com.dayoung.procurement.ledger.exception.DuplicateAccrualReversalException;
import com.dayoung.procurement.ledger.exception.DuplicateAccrualRepostingException;
import com.dayoung.procurement.ledger.exception.InvalidAccrualRepostingTargetException;
import com.dayoung.procurement.ledger.repository.AccrualEntryRepository;
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
import com.dayoung.procurement.purchase.exception.PurchaseRoleRequiredException;
import com.dayoung.procurement.purchase.repository.PurchaseOrderLineRepository;
import com.dayoung.procurement.receipt.application.CreateGoodsReceiptCommand;
import com.dayoung.procurement.receipt.application.CreateGoodsReceiptLineCommand;
import com.dayoung.procurement.receipt.application.GoodsReceiptService;
import com.dayoung.procurement.user.domain.AppUser;
import com.dayoung.procurement.user.domain.Role;
import com.dayoung.procurement.user.domain.RoleCode;
import com.dayoung.procurement.user.domain.UserRole;
import com.dayoung.procurement.user.repository.AppUserRepository;
import com.dayoung.procurement.user.repository.RoleRepository;
import com.dayoung.procurement.user.repository.UserRoleRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class LedgerCorrectionServiceTest {

	private static final DateTimeFormatter PERIOD_FORMAT = DateTimeFormatter.ofPattern("yyyyMM");

	@Autowired
	private LedgerCorrectionService ledgerCorrectionService;
	@Autowired
	private GoodsReceiptService goodsReceiptService;
	@Autowired
	private AccrualEntryRepository accrualEntryRepository;
	@Autowired
	private ClosePeriodRepository closePeriodRepository;
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
	private AuditLogRepository auditLogRepository;

	@Test
	void reversesClosedEntryWithoutChangingOriginal() {
		ReversalScenario scenario = createScenario(LocalDate.of(2002, 1, 15));
		close(scenario.original().getPostingDate());
		AppUser admin = createUser("reversal-admin", RoleCode.ADMIN);

		Long reversalId = ledgerCorrectionService.reverse(
				scenario.original().getId(),
				admin.getId(),
				new ReverseAccrualEntryCommand(LocalDate.now(), "마감 후 입고 금액 오류")
		);

		List<AccrualEntry> entries = accrualEntryRepository
				.findAllByPurchaseOrderLine_IdOrderById(scenario.lineId());
		assertEquals(List.of(AccrualEntryType.GR_ACCRUAL, AccrualEntryType.REVERSAL),
				entries.stream().map(AccrualEntry::getEntryType).toList());
		assertEquals(new BigDecimal("6000.00"), entries.getFirst().getAmount());
		assertEquals(new BigDecimal("-6000.00"), entries.getLast().getAmount());
		assertEquals(scenario.original().getId(), entries.getLast().getReversalOf().getId());
		assertEquals(reversalId, entries.getLast().getId());
		var auditLog = auditLogRepository
				.findAllByTargetTypeAndTargetIdOrderById(AuditTargetType.ACCRUAL_ENTRY, reversalId)
				.getFirst();
		assertEquals(AuditEventType.ACCRUAL_REVERSED, auditLog.getEventType());
		assertEquals("마감 후 입고 금액 오류", auditLog.getReason());
	}

	@Test
	void rejectsReversalForOpenPeriodEntry() {
		ReversalScenario scenario = createScenario(LocalDate.now());
		AppUser admin = createUser("open-reversal-admin", RoleCode.ADMIN);

		assertThrows(OpenPeriodReversalNotAllowedException.class, () -> ledgerCorrectionService.reverse(
				scenario.original().getId(),
				admin.getId(),
				new ReverseAccrualEntryCommand(LocalDate.now(), "열린 기간 역분개 시도")
		));
	}

	@Test
	void rejectsDuplicateReversal() {
		ReversalScenario scenario = createScenario(LocalDate.of(2002, 2, 15));
		close(scenario.original().getPostingDate());
		AppUser admin = createUser("duplicate-reversal-admin", RoleCode.ADMIN);
		ReverseAccrualEntryCommand command = new ReverseAccrualEntryCommand(LocalDate.now(), "중복 역분개 확인");
		ledgerCorrectionService.reverse(scenario.original().getId(), admin.getId(), command);

		assertThrows(DuplicateAccrualReversalException.class, () ->
				ledgerCorrectionService.reverse(scenario.original().getId(), admin.getId(), command));
	}

	@Test
	void rejectsReversalByBuyer() {
		ReversalScenario scenario = createScenario(LocalDate.of(2002, 3, 15));
		close(scenario.original().getPostingDate());

		assertThrows(PurchaseRoleRequiredException.class, () -> ledgerCorrectionService.reverse(
				scenario.original().getId(),
				scenario.buyerId(),
				new ReverseAccrualEntryCommand(LocalDate.now(), "권한 없는 역분개")
		));
	}

	@Test
	void repostsCorrectedAmountWithOriginalSignAndRejectsDuplicate() {
		ReversalScenario scenario = createScenario(LocalDate.of(2002, 4, 15));
		close(scenario.original().getPostingDate());
		AppUser admin = createUser("reposting-admin", RoleCode.ADMIN);
		Long reversalId = ledgerCorrectionService.reverse(
				scenario.original().getId(),
				admin.getId(),
				new ReverseAccrualEntryCommand(LocalDate.now(), "재기표 전 역분개")
		);
		RepostAccrualEntryCommand command = new RepostAccrualEntryCommand(
				new BigDecimal("4000.00"),
				LocalDate.now(),
				"정상 금액 재기표"
		);

		Long correctionId = ledgerCorrectionService.repost(reversalId, admin.getId(), command);

		List<AccrualEntry> entries = accrualEntryRepository
				.findAllByPurchaseOrderLine_IdOrderById(scenario.lineId());
		assertEquals(List.of(
				AccrualEntryType.GR_ACCRUAL,
				AccrualEntryType.REVERSAL,
				AccrualEntryType.CORRECTION
		), entries.stream().map(AccrualEntry::getEntryType).toList());
		assertEquals(new BigDecimal("4000.00"), entries.getLast().getAmount());
		assertEquals(reversalId, entries.getLast().getRepostingOf().getId());
		assertEquals(correctionId, entries.getLast().getId());
		var auditLog = auditLogRepository
				.findAllByTargetTypeAndTargetIdOrderById(AuditTargetType.ACCRUAL_ENTRY, correctionId)
				.getFirst();
		assertEquals(AuditEventType.ACCRUAL_REPOSTED, auditLog.getEventType());
		assertEquals("정상 금액 재기표", auditLog.getReason());

		assertThrows(DuplicateAccrualRepostingException.class, () ->
				ledgerCorrectionService.repost(reversalId, admin.getId(), command));
	}

	@Test
	void rejectsRepostingForOriginalEntry() {
		ReversalScenario scenario = createScenario(LocalDate.now());
		AppUser admin = createUser("invalid-reposting-admin", RoleCode.ADMIN);

		assertThrows(InvalidAccrualRepostingTargetException.class, () -> ledgerCorrectionService.repost(
				scenario.original().getId(),
				admin.getId(),
				new RepostAccrualEntryCommand(
						new BigDecimal("4000.00"),
						LocalDate.now(),
						"잘못된 대상 재기표"
				)
		));
	}

	private ReversalScenario createScenario(LocalDate postingDate) {
		String suffix = Long.toUnsignedString(System.nanoTime(), 36);
		AppUser requester = createUser("reversal-requester-" + suffix, RoleCode.REQUESTER);
		AppUser buyer = createUser("reversal-buyer-" + suffix, RoleCode.BUYER);
		Item item = itemRepository.save(new Item(
				"ITEM-REVERSAL-" + suffix,
				"역분개 테스트 품목",
				null,
				"EA",
				new BigDecimal("1000.00")
		));
		Vendor vendor = vendorRepository.save(new Vendor(
				"V-REVERSAL-" + suffix,
				"역분개 테스트 공급업체",
				suffix
		));
		Warehouse warehouse = warehouseRepository.findByCode("WH-SEOUL").orElseThrow();
		Long requestId = purchaseRequestService.create(
				requester.getId(),
				new CreatePurchaseRequestCommand(
						"역분개 테스트 구매요청",
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
		Long lineId = purchaseOrderLineRepository.findAllByPurchaseOrder_IdOrderByLineNumber(orderId)
				.stream()
				.map(PurchaseOrderLine::getId)
				.findFirst()
				.orElseThrow();
		Long receiptId = goodsReceiptService.create(
				orderId,
				buyer.getId(),
				new CreateGoodsReceiptCommand(
						postingDate,
						List.of(new CreateGoodsReceiptLineCommand(lineId, new BigDecimal("6.000")))
				)
		);
		AccrualEntry original = accrualEntryRepository
				.findAllByGoodsReceiptLine_GoodsReceipt_IdOrderById(receiptId)
				.getFirst();
		return new ReversalScenario(lineId, buyer.getId(), original);
	}

	private void close(LocalDate postingDate) {
		ClosePeriod closePeriod = new ClosePeriod(postingDate.format(PERIOD_FORMAT));
		closePeriod.close(LocalDateTime.now());
		closePeriodRepository.saveAndFlush(closePeriod);
	}

	private AppUser createUser(String prefix, RoleCode roleCode) {
		Department department = departmentRepository.findByCode("IT").orElseThrow();
		String suffix = Long.toUnsignedString(System.nanoTime(), 36);
		AppUser user = appUserRepository.save(new AppUser(
				prefix + "-" + suffix + "@example.com",
				"encoded-password",
				"역분개 테스트",
				department
		));
		Role role = roleRepository.findByCode(roleCode).orElseThrow();
		userRoleRepository.save(new UserRole(user, role));
		return user;
	}

	private record ReversalScenario(Long lineId, Long buyerId, AccrualEntry original) {
	}
}
