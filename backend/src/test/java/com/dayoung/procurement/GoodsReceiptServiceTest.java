package com.dayoung.procurement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dayoung.procurement.audit.domain.AuditEventType;
import com.dayoung.procurement.audit.domain.AuditTargetType;
import com.dayoung.procurement.audit.repository.AuditLogRepository;
import com.dayoung.procurement.closing.domain.ClosePeriod;
import com.dayoung.procurement.closing.exception.ClosedPeriodException;
import com.dayoung.procurement.closing.repository.ClosePeriodRepository;
import com.dayoung.procurement.common.command.CancelDocumentCommand;
import com.dayoung.procurement.invoice.application.CreateInvoiceCommand;
import com.dayoung.procurement.invoice.application.CreateInvoiceLineCommand;
import com.dayoung.procurement.invoice.application.InvoiceService;
import com.dayoung.procurement.masterdata.domain.Department;
import com.dayoung.procurement.masterdata.domain.Item;
import com.dayoung.procurement.masterdata.domain.Vendor;
import com.dayoung.procurement.masterdata.domain.Warehouse;
import com.dayoung.procurement.masterdata.repository.DepartmentRepository;
import com.dayoung.procurement.masterdata.repository.ItemRepository;
import com.dayoung.procurement.masterdata.repository.VendorRepository;
import com.dayoung.procurement.masterdata.repository.WarehouseRepository;
import com.dayoung.procurement.ledger.application.AccrualEntryService;
import com.dayoung.procurement.ledger.domain.AccrualEntry;
import com.dayoung.procurement.ledger.domain.AccrualEntryType;
import com.dayoung.procurement.ledger.repository.AccrualEntryRepository;
import com.dayoung.procurement.purchase.application.CreatePurchaseOrderCommand;
import com.dayoung.procurement.purchase.application.CreatePurchaseRequestCommand;
import com.dayoung.procurement.purchase.application.CreatePurchaseRequestLineCommand;
import com.dayoung.procurement.purchase.application.PurchaseOrderService;
import com.dayoung.procurement.purchase.application.PurchaseRequestService;
import com.dayoung.procurement.purchase.domain.PurchaseOrder;
import com.dayoung.procurement.purchase.domain.PurchaseOrderLine;
import com.dayoung.procurement.purchase.domain.PurchaseOrderStatus;
import com.dayoung.procurement.purchase.exception.InvalidPurchaseOrderStateException;
import com.dayoung.procurement.purchase.repository.PurchaseOrderLineRepository;
import com.dayoung.procurement.purchase.repository.PurchaseOrderRepository;
import com.dayoung.procurement.receipt.application.CreateGoodsReceiptCommand;
import com.dayoung.procurement.receipt.application.CreateGoodsReceiptLineCommand;
import com.dayoung.procurement.receipt.application.GoodsReceiptService;
import com.dayoung.procurement.receipt.domain.GoodsReceiptStatus;
import com.dayoung.procurement.receipt.exception.GoodsReceiptCancellationBlockedException;
import com.dayoung.procurement.receipt.exception.PurchaseOrderQuantityExceededException;
import com.dayoung.procurement.receipt.repository.GoodsReceiptLineRepository;
import com.dayoung.procurement.receipt.repository.GoodsReceiptRepository;
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
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class GoodsReceiptServiceTest {

	private static final DateTimeFormatter PERIOD_FORMAT = DateTimeFormatter.ofPattern("yyyyMM");

	@Autowired
	private GoodsReceiptService goodsReceiptService;

	@Autowired
	private InvoiceService invoiceService;

	@Autowired
	private AccrualEntryService accrualEntryService;

	@Autowired
	private AccrualEntryRepository accrualEntryRepository;

	@Autowired
	private ClosePeriodRepository closePeriodRepository;

	@Autowired
	private PurchaseOrderService purchaseOrderService;

	@Autowired
	private PurchaseRequestService purchaseRequestService;

	@Autowired
	private GoodsReceiptRepository goodsReceiptRepository;

	@Autowired
	private GoodsReceiptLineRepository goodsReceiptLineRepository;

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
	private AuditLogRepository auditLogRepository;

	@Test
	void changesOrderStatusAfterPartialAndFullReceipts() {
		TestOrder testOrder = createSentOrder("receipt-state");
		long initialReceiptCount = goodsReceiptRepository.count();

		Long firstReceiptId = goodsReceiptService.create(
				testOrder.orderId(),
				testOrder.buyerId(),
				createCommand(testOrder.lineId(), "6.000")
		);

		PurchaseOrder partiallyReceived = purchaseOrderRepository.findById(testOrder.orderId()).orElseThrow();
		assertEquals(PurchaseOrderStatus.PARTIALLY_RECEIVED, partiallyReceived.getStatus());
		assertEquals(new BigDecimal("6.000"), getPostedQuantity(testOrder.lineId()));
		List<AccrualEntry> firstEntries = accrualEntryRepository
				.findAllByGoodsReceiptLine_GoodsReceipt_IdOrderById(firstReceiptId);
		assertEquals(1, firstEntries.size());
		assertEquals(AccrualEntryType.GR_ACCRUAL, firstEntries.getFirst().getEntryType());
		assertEquals(new BigDecimal("6000.00"), firstEntries.getFirst().getAmount());
		assertEquals(LocalDate.now().format(PERIOD_FORMAT),
				firstEntries.getFirst().getPeriod());

		Long secondReceiptId = goodsReceiptService.create(
				testOrder.orderId(),
				testOrder.buyerId(),
				createCommand(testOrder.lineId(), "4.000")
		);

		PurchaseOrder received = purchaseOrderRepository.findById(testOrder.orderId()).orElseThrow();
		assertEquals(PurchaseOrderStatus.RECEIVED, received.getStatus());
		assertEquals(new BigDecimal("10.000"), getPostedQuantity(testOrder.lineId()));
		assertEquals(initialReceiptCount + 2, goodsReceiptRepository.count());
		assertEquals(1, accrualEntryRepository
				.findAllByGoodsReceiptLine_GoodsReceipt_IdOrderById(secondReceiptId)
				.size());

		long accrualCount = accrualEntryRepository.count();
		accrualEntryService.createForGoodsReceipt(firstReceiptId);
		assertEquals(accrualCount, accrualEntryRepository.count());
	}

	@Test
	void rejectsQuantityExceedingPurchaseOrderAndKeepsSavedState() {
		TestOrder testOrder = createSentOrder("receipt-over");
		goodsReceiptService.create(
				testOrder.orderId(),
				testOrder.buyerId(),
				createCommand(testOrder.lineId(), "6.000")
		);
		long receiptCount = goodsReceiptRepository.count();
		long accrualCount = accrualEntryRepository.count();

		assertThrows(PurchaseOrderQuantityExceededException.class, () ->
				goodsReceiptService.create(
						testOrder.orderId(),
						testOrder.buyerId(),
						createCommand(testOrder.lineId(), "5.000")
				)
		);

		PurchaseOrder order = purchaseOrderRepository.findById(testOrder.orderId()).orElseThrow();
		assertEquals(PurchaseOrderStatus.PARTIALLY_RECEIVED, order.getStatus());
		assertEquals(new BigDecimal("6.000"), getPostedQuantity(testOrder.lineId()));
		assertEquals(receiptCount, goodsReceiptRepository.count());
		assertEquals(accrualCount, accrualEntryRepository.count());
	}

	@Test
	void cancelsOpenPeriodReceiptWithOffsetAndRestoresOrderState() {
		TestOrder testOrder = createSentOrder("receipt-cancel");
		Long receiptId = goodsReceiptService.create(
				testOrder.orderId(),
				testOrder.buyerId(),
				createCommand(testOrder.lineId(), "6.000")
		);

		goodsReceiptService.cancel(
				testOrder.orderId(),
				receiptId,
				testOrder.buyerId(),
				new CancelDocumentCommand(LocalDate.now(), "입고 수량 오류")
		);

		GoodsReceiptStatus status = goodsReceiptRepository.findById(receiptId).orElseThrow().getStatus();
		PurchaseOrder order = purchaseOrderRepository.findById(testOrder.orderId()).orElseThrow();
		List<AccrualEntry> entries = accrualEntryRepository
				.findAllByPurchaseOrderLine_IdOrderById(testOrder.lineId());

		assertEquals(GoodsReceiptStatus.CANCELLED, status);
		assertEquals(PurchaseOrderStatus.SENT, order.getStatus());
		assertEquals(new BigDecimal("0.000"), getPostedQuantity(testOrder.lineId()));
		assertEquals(List.of(AccrualEntryType.GR_ACCRUAL, AccrualEntryType.CANCEL_OFFSET),
				entries.stream().map(AccrualEntry::getEntryType).toList());
		assertEquals(new BigDecimal("6000.00"), entries.getFirst().getAmount());
		assertEquals(new BigDecimal("-6000.00"), entries.getLast().getAmount());
		assertEquals(entries.getFirst().getId(), entries.getLast().getReversalOf().getId());
		var auditLog = auditLogRepository
				.findAllByTargetTypeAndTargetIdOrderById(AuditTargetType.GOODS_RECEIPT, receiptId)
				.getFirst();
		assertEquals(AuditEventType.GOODS_RECEIPT_CANCELLED, auditLog.getEventType());
		assertEquals(testOrder.buyerId(), auditLog.getActor().getId());
		assertEquals("입고 수량 오류", auditLog.getReason());
	}

	@Test
	void rejectsReceiptCancellationInClosedPeriodWithoutOffset() {
		TestOrder testOrder = createSentOrder("receipt-cancel-closed");
		LocalDate postingDate = LocalDate.of(2001, 1, 15);
		Long receiptId = goodsReceiptService.create(
				testOrder.orderId(),
				testOrder.buyerId(),
				createCommand(testOrder.lineId(), "6.000", postingDate)
		);
		ClosePeriod closePeriod = new ClosePeriod(postingDate.format(PERIOD_FORMAT));
		closePeriod.close(LocalDateTime.now());
		closePeriodRepository.save(closePeriod);

		assertThrows(ClosedPeriodException.class, () -> goodsReceiptService.cancel(
				testOrder.orderId(),
				receiptId,
				testOrder.buyerId(),
				new CancelDocumentCommand(postingDate, "마감 후 취소 시도")
		));

		assertEquals(GoodsReceiptStatus.POSTED,
				goodsReceiptRepository.findById(receiptId).orElseThrow().getStatus());
		assertEquals(List.of(AccrualEntryType.GR_ACCRUAL), accrualEntryRepository
				.findAllByPurchaseOrderLine_IdOrderById(testOrder.lineId()).stream()
				.map(AccrualEntry::getEntryType)
				.toList());
	}

	@Test
	void rejectsReceiptCancellationWhenActiveInvoiceWouldExceedRemainingReceipt() {
		TestOrder testOrder = createSentOrder("receipt-cancel-invoiced");
		Long receiptId = goodsReceiptService.create(
				testOrder.orderId(),
				testOrder.buyerId(),
				createCommand(testOrder.lineId(), "6.000")
		);
		invoiceService.create(
				testOrder.orderId(),
				testOrder.buyerId(),
				new CreateInvoiceCommand(
						"INV-RECEIPT-CANCEL-" + System.nanoTime(),
						LocalDate.now(),
						LocalDate.now(),
						List.of(new CreateInvoiceLineCommand(
								testOrder.lineId(),
								new BigDecimal("6.000"),
								new BigDecimal("1000.00")
						))
				)
		);

		assertThrows(GoodsReceiptCancellationBlockedException.class, () -> goodsReceiptService.cancel(
				testOrder.orderId(),
				receiptId,
				testOrder.buyerId(),
				new CancelDocumentCommand(LocalDate.now(), "송장 연결 입고 취소 시도")
		));
		assertEquals(GoodsReceiptStatus.POSTED,
				goodsReceiptRepository.findById(receiptId).orElseThrow().getStatus());
	}

	@Test
	void rejectsReceiptForCreatedPurchaseOrder() {
		TestOrder testOrder = createOrder("receipt-created", false);

		assertThrows(InvalidPurchaseOrderStateException.class, () ->
				goodsReceiptService.create(
						testOrder.orderId(),
						testOrder.buyerId(),
						createCommand(testOrder.lineId(), "1.000")
				)
		);
	}

	@Test
	void rejectsReceiptInClosedPeriodAndKeepsSavedState() {
		TestOrder testOrder = createSentOrder("receipt-closed-period");
		LocalDate postingDate = LocalDate.now().minusMonths(1).withDayOfMonth(15);
		ClosePeriod closePeriod = new ClosePeriod(postingDate.format(PERIOD_FORMAT));
		closePeriod.close(LocalDateTime.now());
		closePeriodRepository.save(closePeriod);
		long receiptCount = goodsReceiptRepository.count();
		long accrualCount = accrualEntryRepository.count();

		assertThrows(ClosedPeriodException.class, () ->
				goodsReceiptService.create(
						testOrder.orderId(),
						testOrder.buyerId(),
						createCommand(testOrder.lineId(), "1.000", postingDate)
				)
		);

		PurchaseOrder order = purchaseOrderRepository.findById(testOrder.orderId()).orElseThrow();
		assertEquals(PurchaseOrderStatus.SENT, order.getStatus());
		assertEquals(receiptCount, goodsReceiptRepository.count());
		assertEquals(accrualCount, accrualEntryRepository.count());
	}

	@Test
	void allowsOnlyOneConcurrentReceiptForLastRemainingQuantity() throws Exception {
		TestOrder testOrder = createSentOrder("receipt-concurrent");
		goodsReceiptService.create(
				testOrder.orderId(),
				testOrder.buyerId(),
				createCommand(testOrder.lineId(), "6.000")
		);
		long receiptCount = goodsReceiptRepository.count();
		long accrualCount = accrualEntryRepository.count();
		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(2);

		try {
			Callable<Boolean> receiveRemainingQuantity = () -> {
				ready.countDown();
				start.await();
				try {
					goodsReceiptService.create(
							testOrder.orderId(),
							testOrder.buyerId(),
							createCommand(testOrder.lineId(), "4.000")
					);
					return true;
				} catch (InvalidPurchaseOrderStateException | PurchaseOrderQuantityExceededException exception) {
					return false;
				}
			};
			Future<Boolean> firstResult = executor.submit(receiveRemainingQuantity);
			Future<Boolean> secondResult = executor.submit(receiveRemainingQuantity);
			assertTrue(ready.await(5, TimeUnit.SECONDS));
			start.countDown();

			long successCount = (firstResult.get(20, TimeUnit.SECONDS) ? 1 : 0)
					+ (secondResult.get(20, TimeUnit.SECONDS) ? 1 : 0);
			assertEquals(1, successCount);
		} finally {
			executor.shutdownNow();
		}

		PurchaseOrder order = purchaseOrderRepository.findById(testOrder.orderId()).orElseThrow();
		assertEquals(PurchaseOrderStatus.RECEIVED, order.getStatus());
		assertEquals(new BigDecimal("10.000"), getPostedQuantity(testOrder.lineId()));
		assertEquals(receiptCount + 1, goodsReceiptRepository.count());
		assertEquals(accrualCount + 1, accrualEntryRepository.count());
	}

	private TestOrder createSentOrder(String suffix) {
		return createOrder(suffix, true);
	}

	private TestOrder createOrder(String suffix, boolean send) {
		AppUser requester = createUser("requester-" + suffix + "@example.com", RoleCode.REQUESTER);
		AppUser buyer = createUser("buyer-" + suffix + "@example.com", RoleCode.BUYER);
		Item item = itemRepository.save(new Item(
				"ITEM-" + suffix,
				"입고 테스트 품목",
				null,
				"EA",
				new BigDecimal("1000.00")
		));
		String vendorSuffix = String.valueOf(System.nanoTime());
		Vendor vendor = vendorRepository.save(new Vendor(
				"V-" + vendorSuffix,
				"입고 테스트 공급업체",
				vendorSuffix
		));
		Warehouse warehouse = warehouseRepository.findByCode("WH-SEOUL").orElseThrow();
		Long requestId = purchaseRequestService.create(
				requester.getId(),
				new CreatePurchaseRequestCommand(
						"입고 테스트 구매요청",
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
		if (send) {
			purchaseOrderService.send(orderId, buyer.getId());
		}
		PurchaseOrderLine line = purchaseOrderLineRepository
				.findAllByPurchaseOrder_IdOrderByLineNumber(orderId)
				.getFirst();
		return new TestOrder(orderId, line.getId(), buyer.getId());
	}

	private AppUser createUser(String email, RoleCode roleCode) {
		Department department = departmentRepository.findByCode("IT").orElseThrow();
		AppUser user = appUserRepository.save(new AppUser(email, "encoded-password", "입고 테스트", department));
		Role role = roleRepository.findByCode(roleCode).orElseThrow();
		userRoleRepository.save(new UserRole(user, role));
		return user;
	}

	private CreateGoodsReceiptCommand createCommand(Long lineId, String quantity) {
		return createCommand(lineId, quantity, LocalDate.now());
	}

	private CreateGoodsReceiptCommand createCommand(Long lineId, String quantity, LocalDate postingDate) {
		return new CreateGoodsReceiptCommand(
				postingDate,
				List.of(new CreateGoodsReceiptLineCommand(lineId, new BigDecimal(quantity)))
		);
	}

	private BigDecimal getPostedQuantity(Long lineId) {
		return goodsReceiptLineRepository.sumQuantityByPurchaseOrderLineIdAndStatus(
				lineId,
				GoodsReceiptStatus.POSTED
		);
	}

	private record TestOrder(Long orderId, Long lineId, Long buyerId) {
	}
}
