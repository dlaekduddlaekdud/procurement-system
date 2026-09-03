package com.dayoung.procurement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;

import com.dayoung.procurement.common.command.CancelDocumentCommand;
import com.dayoung.procurement.invoice.application.CreateInvoiceCommand;
import com.dayoung.procurement.invoice.application.CreateInvoiceLineCommand;
import com.dayoung.procurement.invoice.application.InvoiceService;
import com.dayoung.procurement.invoice.domain.Invoice;
import com.dayoung.procurement.invoice.domain.InvoiceLine;
import com.dayoung.procurement.invoice.domain.InvoiceStatus;
import com.dayoung.procurement.invoice.exception.DuplicateInvoiceException;
import com.dayoung.procurement.invoice.repository.InvoiceLineRepository;
import com.dayoung.procurement.invoice.repository.InvoiceRepository;
import com.dayoung.procurement.ledger.domain.AccrualEntry;
import com.dayoung.procurement.ledger.domain.AccrualEntryType;
import com.dayoung.procurement.ledger.repository.AccrualEntryRepository;
import com.dayoung.procurement.masterdata.domain.Department;
import com.dayoung.procurement.masterdata.domain.Item;
import com.dayoung.procurement.masterdata.domain.Vendor;
import com.dayoung.procurement.masterdata.domain.Warehouse;
import com.dayoung.procurement.masterdata.repository.DepartmentRepository;
import com.dayoung.procurement.masterdata.repository.ItemRepository;
import com.dayoung.procurement.masterdata.repository.VendorRepository;
import com.dayoung.procurement.masterdata.repository.WarehouseRepository;
import com.dayoung.procurement.matching.application.MatchingLineTotals;
import com.dayoung.procurement.matching.application.ThreeWayMatchingService;
import com.dayoung.procurement.matching.domain.MatchResult;
import com.dayoung.procurement.matching.domain.MatchingStatus;
import com.dayoung.procurement.matching.repository.MatchResultRepository;
import com.dayoung.procurement.purchase.application.CreatePurchaseOrderCommand;
import com.dayoung.procurement.purchase.application.CreatePurchaseRequestCommand;
import com.dayoung.procurement.purchase.application.CreatePurchaseRequestLineCommand;
import com.dayoung.procurement.purchase.application.PurchaseOrderService;
import com.dayoung.procurement.purchase.application.PurchaseRequestService;
import com.dayoung.procurement.purchase.domain.PurchaseOrderLine;
import com.dayoung.procurement.purchase.exception.InvalidPurchaseOrderStateException;
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
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class InvoiceServiceTest {

	@Autowired
	private InvoiceService invoiceService;

	@Autowired
	private InvoiceRepository invoiceRepository;

	@Autowired
	private InvoiceLineRepository invoiceLineRepository;

	@Autowired
	private GoodsReceiptService goodsReceiptService;

	@MockitoSpyBean
	private ThreeWayMatchingService threeWayMatchingService;

	@Autowired
	private MatchResultRepository matchResultRepository;

	@Autowired
	private AccrualEntryRepository accrualEntryRepository;

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

	@Test
	void createsInvoiceWithLineLevelTaxAndHeaderTotals() {
		TestOrder testOrder = createOrder("invoice-amount", true);

		Long invoiceId = invoiceService.create(
				testOrder.orderId(),
				testOrder.buyerId(),
				createCommand("INV-AMOUNT-001", testOrder.lineIds())
		);

		Invoice invoice = invoiceRepository.findById(invoiceId).orElseThrow();
		List<InvoiceLine> lines = invoiceLineRepository.findAllByInvoice_IdOrderByLineNumber(invoiceId);
		assertEquals(new BigDecimal("30.00"), invoice.getSupplyAmount());
		assertEquals(new BigDecimal("2.00"), invoice.getTaxAmount());
		assertEquals(new BigDecimal("32.00"), invoice.getTotalAmount());
		assertEquals(2, lines.size());
		assertEquals(new BigDecimal("1.00"), lines.getFirst().getTaxAmount());
		assertEquals(new BigDecimal("1.00"), lines.getLast().getTaxAmount());
	}

	@Test
	void rejectsDuplicateInvoiceNumberForSameVendor() {
		TestOrder testOrder = createOrder("invoice-duplicate", true);
		CreateInvoiceCommand command = createCommand("INV-DUPLICATE-001", testOrder.lineIds());
		invoiceService.create(testOrder.orderId(), testOrder.buyerId(), command);

		assertThrows(DuplicateInvoiceException.class,
				() -> invoiceService.create(testOrder.orderId(), testOrder.buyerId(), command));
	}

	@Test
	void cancelsMatchedInvoiceAndOffsetsOnlyItsSettlement() {
		TestOrder testOrder = createOrder("invoice-cancel", true);
		Long lineId = testOrder.lineIds().getFirst();
		goodsReceiptService.create(
				testOrder.orderId(),
				testOrder.buyerId(),
				createReceiptCommand(lineId, "1.000")
		);
		Long invoiceId = invoiceService.create(
				testOrder.orderId(),
				testOrder.buyerId(),
				createSingleLineInvoiceCommand("INV-CANCEL-001", lineId, "1.000")
		);

		invoiceService.cancel(
				testOrder.orderId(),
				invoiceId,
				testOrder.buyerId(),
				new CancelDocumentCommand(LocalDate.now())
		);

		Invoice invoice = invoiceRepository.findById(invoiceId).orElseThrow();
		List<AccrualEntry> entries = accrualEntryRepository.findAllByPurchaseOrderLine_IdOrderById(lineId);
		assertEquals(InvoiceStatus.CANCELLED, invoice.getStatus());
		assertEquals(
				List.of(
						AccrualEntryType.GR_ACCRUAL,
						AccrualEntryType.INVOICE_MATCH,
						AccrualEntryType.CANCEL_OFFSET
				),
				entries.stream().map(AccrualEntry::getEntryType).toList()
		);
		assertEquals(new BigDecimal("1000.00"), entries.getLast().getAmount());
		assertEquals(entries.get(1).getId(), entries.getLast().getReversalOf().getId());
		assertEquals(new BigDecimal("1000.00"), accrualBalance(lineId));
		assertEquals(MatchingStatus.HOLD_QUANTITY,
				matchResultRepository.findByPurchaseOrderLine_Id(lineId).orElseThrow().getStatus());
	}

	@Test
	void rejectsInvoiceForCreatedPurchaseOrder() {
		TestOrder testOrder = createOrder("invoice-created", false);

		assertThrows(InvalidPurchaseOrderStateException.class, () ->
				invoiceService.create(
						testOrder.orderId(),
						testOrder.buyerId(),
						createCommand("INV-CREATED-001", testOrder.lineIds())
				)
		);
	}

	@Test
	void calculatesCumulativeReceiptAndInvoiceTotalsByPurchaseOrderLine() {
		TestOrder testOrder = createOrder("matching-totals", true);
		Long lineId = testOrder.lineIds().getFirst();
		goodsReceiptService.create(
				testOrder.orderId(),
				testOrder.buyerId(),
				createReceiptCommand(lineId, "0.400")
		);
		goodsReceiptService.create(
				testOrder.orderId(),
				testOrder.buyerId(),
				createReceiptCommand(lineId, "0.600")
		);
		invoiceService.create(
				testOrder.orderId(),
				testOrder.buyerId(),
				createSingleLineInvoiceCommand("INV-SPLIT-001", lineId, "0.300")
		);
		invoiceService.create(
				testOrder.orderId(),
				testOrder.buyerId(),
				createSingleLineInvoiceCommand("INV-SPLIT-002", lineId, "0.700")
		);

		MatchingLineTotals totals = threeWayMatchingService.calculateTotals(lineId);

		assertEquals(new BigDecimal("1.000"), totals.orderedQuantity());
		assertEquals(new BigDecimal("1.000"), totals.receivedQuantity());
		assertEquals(new BigDecimal("1.000"), totals.invoicedQuantity());
		assertEquals(new BigDecimal("1000.00"), totals.orderedAmount());
		assertEquals(new BigDecimal("1000.00"), totals.invoicedAmount());
	}

	@Test
	void automaticallyRematchesAndSettlesHoldWhenAdditionalReceiptResolvesIt() {
		TestOrder testOrder = createOrder("matching-result", true);
		Long lineId = testOrder.lineIds().getFirst();
		goodsReceiptService.create(
				testOrder.orderId(),
				testOrder.buyerId(),
				createReceiptCommand(lineId, "0.800")
		);
		invoiceService.create(
				testOrder.orderId(),
				testOrder.buyerId(),
				createSingleLineInvoiceCommand("INV-HOLD-001", lineId, "1.000")
		);

		Long resultId = threeWayMatchingService.evaluateAndSave(lineId);
		MatchResult hold = matchResultRepository.findById(resultId).orElseThrow();
		assertEquals(MatchingStatus.HOLD_QUANTITY, hold.getStatus());
		assertEquals(new BigDecimal("0.800"), hold.getReceivedQuantity());
		assertEquals(
				List.of(AccrualEntryType.GR_ACCRUAL),
				accrualEntryRepository.findAllByPurchaseOrderLine_IdOrderById(lineId).stream()
						.map(AccrualEntry::getEntryType)
						.toList()
		);

		goodsReceiptService.create(
				testOrder.orderId(),
				testOrder.buyerId(),
				createReceiptCommand(lineId, "0.200")
		);
		MatchResult resolved = matchResultRepository.findById(resultId).orElseThrow();
		List<AccrualEntry> entries = accrualEntryRepository.findAllByPurchaseOrderLine_IdOrderById(lineId);

		assertEquals(MatchingStatus.MATCHED, resolved.getStatus());
		assertEquals(new BigDecimal("1.000"), resolved.getReceivedQuantity());
		assertEquals(1, matchResultRepository.count());
		assertEquals(3, entries.size());
		assertEquals(
				List.of(
						AccrualEntryType.GR_ACCRUAL,
						AccrualEntryType.GR_ACCRUAL,
						AccrualEntryType.INVOICE_MATCH
				),
				entries.stream().map(AccrualEntry::getEntryType).toList()
		);
		assertEquals(new BigDecimal("-1000.00"), entries.getLast().getAmount());
	}

	@Test
	void settlesOnlyNewAmountAfterPreviousPartialSettlement() {
		TestOrder testOrder = createOrder("matching-additional-entry", true);
		Long lineId = testOrder.lineIds().getFirst();
		goodsReceiptService.create(
				testOrder.orderId(),
				testOrder.buyerId(),
				createReceiptCommand(lineId, "0.990")
		);
		invoiceService.create(
				testOrder.orderId(),
				testOrder.buyerId(),
				createSingleLineInvoiceCommand("INV-MATCH-PARTIAL-001", lineId, "0.990")
		);

		goodsReceiptService.create(
				testOrder.orderId(),
				testOrder.buyerId(),
				createReceiptCommand(lineId, "0.010")
		);
		// 추가 입고분에 대한 송장이 아직 없어도 기존 송장분은 안분 기준으로 대사 상태를 유지한다.
		assertEquals(
				MatchingStatus.MATCHED,
				matchResultRepository.findByPurchaseOrderLine_Id(lineId).orElseThrow().getStatus()
		);

		invoiceService.create(
				testOrder.orderId(),
				testOrder.buyerId(),
				createSingleLineInvoiceCommand("INV-MATCH-PARTIAL-002", lineId, "0.010")
		);
		threeWayMatchingService.matchAndSettle(lineId);

		List<AccrualEntry> matchingEntries = accrualEntryRepository
				.findAllByPurchaseOrderLine_IdOrderById(lineId)
				.stream()
				.filter(entry -> entry.getEntryType() == AccrualEntryType.INVOICE_MATCH)
				.toList();
		assertEquals(MatchingStatus.MATCHED,
				matchResultRepository.findByPurchaseOrderLine_Id(lineId).orElseThrow().getStatus());
		assertEquals(2, matchingEntries.size());
		assertEquals(new BigDecimal("-990.00"), matchingEntries.getFirst().getAmount());
		assertEquals(new BigDecimal("-10.00"), matchingEntries.getLast().getAmount());
	}

	@Test
	void settlesPartialInvoiceImmediatelyAgainstProratedAmount() {
		TestOrder testOrder = createOrder("matching-prorated", true);
		Long lineId = testOrder.lineIds().getFirst();
		goodsReceiptService.create(
				testOrder.orderId(),
				testOrder.buyerId(),
				createReceiptCommand(lineId, "0.600")
		);
		invoiceService.create(
				testOrder.orderId(),
				testOrder.buyerId(),
				createSingleLineInvoiceCommand("INV-PRORATED-001", lineId, "0.600")
		);

		assertEquals(
				MatchingStatus.MATCHED,
				matchResultRepository.findByPurchaseOrderLine_Id(lineId).orElseThrow().getStatus()
		);
		assertEquals(List.of(new BigDecimal("-600.00")), settledAmounts(lineId));

		goodsReceiptService.create(
				testOrder.orderId(),
				testOrder.buyerId(),
				createReceiptCommand(lineId, "0.400")
		);
		invoiceService.create(
				testOrder.orderId(),
				testOrder.buyerId(),
				createSingleLineInvoiceCommand("INV-PRORATED-002", lineId, "0.400")
		);

		assertEquals(
				MatchingStatus.MATCHED,
				matchResultRepository.findByPurchaseOrderLine_Id(lineId).orElseThrow().getStatus()
		);
		assertEquals(
				List.of(new BigDecimal("-600.00"), new BigDecimal("-400.00")),
				settledAmounts(lineId)
		);
		// 부분 입고와 부분 송장이 같은 속도로 계상·상계되므로 미착 잔액이 남지 않는다.
		assertEquals(0, accrualBalance(lineId).signum());
	}

	private List<BigDecimal> settledAmounts(Long lineId) {
		return accrualEntryRepository.findAllByPurchaseOrderLine_IdOrderById(lineId).stream()
				.filter(entry -> entry.getEntryType() == AccrualEntryType.INVOICE_MATCH)
				.map(AccrualEntry::getAmount)
				.toList();
	}

	private BigDecimal accrualBalance(Long lineId) {
		return accrualEntryRepository.findAllByPurchaseOrderLine_IdOrderById(lineId).stream()
				.map(AccrualEntry::getAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	@Test
	void createsInvoiceMatchEntryOnlyOnceWhenMatchingSucceeds() {
		TestOrder testOrder = createOrder("matching-entry", true);
		Long lineId = testOrder.lineIds().getFirst();
		goodsReceiptService.create(
				testOrder.orderId(),
				testOrder.buyerId(),
				createReceiptCommand(lineId, "1.000")
		);

		invoiceService.create(
				testOrder.orderId(),
				testOrder.buyerId(),
				createSingleLineInvoiceCommand("INV-MATCH-001", lineId, "1.000")
		);

		List<AccrualEntry> entries = accrualEntryRepository.findAllByPurchaseOrderLine_IdOrderById(lineId);
		assertEquals(2, entries.size());
		assertEquals(AccrualEntryType.GR_ACCRUAL, entries.getFirst().getEntryType());
		assertEquals(new BigDecimal("1000.00"), entries.getFirst().getAmount());
		assertEquals(AccrualEntryType.INVOICE_MATCH, entries.getLast().getEntryType());
		assertEquals(new BigDecimal("-1000.00"), entries.getLast().getAmount());

		threeWayMatchingService.matchAndSettle(lineId);
		assertEquals(2, accrualEntryRepository.findAllByPurchaseOrderLine_IdOrderById(lineId).size());
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	void rollsBackInvoiceMatchResultAndSettlementWhenMatchingFails() {
		TestOrder testOrder = createOrder("matching-rollback", true);
		Long lineId = testOrder.lineIds().getFirst();
		goodsReceiptService.create(
				testOrder.orderId(),
				testOrder.buyerId(),
				createReceiptCommand(lineId, "1.000")
		);
		long invoiceCountBefore = invoiceRepository.count();
		long invoiceLineCountBefore = invoiceLineRepository.count();
		AtomicBoolean matchingChangesCreated = new AtomicBoolean();

		doAnswer(invocation -> {
			invocation.callRealMethod();
			boolean resultCreated = matchResultRepository.findByPurchaseOrderLine_Id(lineId).isPresent();
			long settlementCount = accrualEntryRepository.findAllByPurchaseOrderLine_IdOrderById(lineId)
					.stream()
					.filter(entry -> entry.getEntryType() == AccrualEntryType.INVOICE_MATCH)
					.count();
			matchingChangesCreated.set(resultCreated && settlementCount == 1);
			throw new IllegalStateException("대사 완료 후 실패 상황 재현");
		}).when(threeWayMatchingService).matchAndSettle(eq(lineId));

		assertThrows(
				IllegalStateException.class,
				() -> invoiceService.create(
						testOrder.orderId(),
						testOrder.buyerId(),
						createSingleLineInvoiceCommand("INV-MATCH-ROLLBACK-001", lineId, "1.000")
				)
		);

		assertTrue(matchingChangesCreated.get());
		assertEquals(invoiceCountBefore, invoiceRepository.count());
		assertEquals(invoiceLineCountBefore, invoiceLineRepository.count());
		assertTrue(matchResultRepository.findByPurchaseOrderLine_Id(lineId).isEmpty());
		assertEquals(
				List.of(AccrualEntryType.GR_ACCRUAL),
				accrualEntryRepository.findAllByPurchaseOrderLine_IdOrderById(lineId).stream()
						.map(AccrualEntry::getEntryType)
						.toList()
		);
	}

	private TestOrder createOrder(String testName, boolean send) {
		String suffix = testName + "-" + Long.toUnsignedString(System.nanoTime(), 36);
		AppUser requester = createUser("requester-" + suffix + "@example.com", RoleCode.REQUESTER);
		AppUser buyer = createUser("buyer-" + suffix + "@example.com", RoleCode.BUYER);
		Item firstItem = createItem("ITEM-FIRST-" + suffix);
		Item secondItem = createItem("ITEM-SECOND-" + suffix);
		Vendor vendor = vendorRepository.save(new Vendor(
				"V-INVOICE-" + System.nanoTime(),
				"송장 테스트 공급업체",
				String.valueOf(System.nanoTime())
		));
		Warehouse warehouse = warehouseRepository.findByCode("WH-SEOUL").orElseThrow();
		Long requestId = purchaseRequestService.create(
				requester.getId(),
				new CreatePurchaseRequestCommand(
						"송장 테스트 구매요청",
						null,
						LocalDate.now(),
						List.of(
								createRequestLine(firstItem.getId()),
								createRequestLine(secondItem.getId())
						)
				)
		);
		purchaseRequestService.submit(requestId, requester.getId());
		purchaseRequestService.approve(requestId, buyer.getId());
		Long orderId = purchaseOrderService.createFromApprovedRequest(
				requestId,
				buyer.getId(),
				new CreatePurchaseOrderCommand(vendor.getId(), warehouse.getId(), LocalDate.now())
		);
		if (send) {
			purchaseOrderService.send(orderId, buyer.getId());
		}
		List<Long> lineIds = purchaseOrderLineRepository.findAllByPurchaseOrder_IdOrderByLineNumber(orderId)
				.stream()
				.map(PurchaseOrderLine::getId)
				.toList();
		return new TestOrder(orderId, buyer.getId(), lineIds);
	}

	private Item createItem(String code) {
		return itemRepository.save(new Item(code, "송장 테스트 품목", null, "EA", new BigDecimal("1000.00")));
	}

	private CreatePurchaseRequestLineCommand createRequestLine(Long itemId) {
		return new CreatePurchaseRequestLineCommand(
				itemId,
				BigDecimal.ONE,
				new BigDecimal("1000.00"),
				null
		);
	}

	private CreateInvoiceCommand createCommand(String invoiceNumber, List<Long> lineIds) {
		return new CreateInvoiceCommand(
				invoiceNumber,
				LocalDate.now(),
				LocalDate.now(),
				lineIds.stream()
						.map(lineId -> new CreateInvoiceLineCommand(
								lineId,
								BigDecimal.ONE,
								new BigDecimal("15.00")
						))
						.toList()
		);
	}

	private CreateInvoiceCommand createSingleLineInvoiceCommand(
			String invoiceNumber,
			Long lineId,
			String quantity
	) {
		return new CreateInvoiceCommand(
				invoiceNumber,
				LocalDate.now(),
				LocalDate.now(),
				List.of(new CreateInvoiceLineCommand(
						lineId,
						new BigDecimal(quantity),
						new BigDecimal("1000.00")
				))
		);
	}

	private CreateGoodsReceiptCommand createReceiptCommand(Long lineId, String quantity) {
		return new CreateGoodsReceiptCommand(
				LocalDate.now(),
				List.of(new CreateGoodsReceiptLineCommand(lineId, new BigDecimal(quantity)))
		);
	}

	private AppUser createUser(String email, RoleCode roleCode) {
		Department department = departmentRepository.findByCode("IT").orElseThrow();
		AppUser user = appUserRepository.save(new AppUser(email, "encoded-password", "송장 테스트", department));
		Role role = roleRepository.findByCode(roleCode).orElseThrow();
		userRoleRepository.save(new UserRole(user, role));
		return user;
	}

	private record TestOrder(Long orderId, Long buyerId, List<Long> lineIds) {
	}
}
