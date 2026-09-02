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
import com.dayoung.procurement.purchase.domain.PurchaseOrderLine;
import com.dayoung.procurement.purchase.domain.PurchaseOrderStatus;
import com.dayoung.procurement.purchase.exception.InvalidPurchaseOrderStateException;
import com.dayoung.procurement.purchase.repository.PurchaseOrderLineRepository;
import com.dayoung.procurement.purchase.repository.PurchaseOrderRepository;
import com.dayoung.procurement.receipt.application.CreateGoodsReceiptCommand;
import com.dayoung.procurement.receipt.application.CreateGoodsReceiptLineCommand;
import com.dayoung.procurement.receipt.application.GoodsReceiptService;
import com.dayoung.procurement.receipt.domain.GoodsReceiptStatus;
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
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class GoodsReceiptServiceTest {

	@Autowired
	private GoodsReceiptService goodsReceiptService;

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

	@Test
	void changesOrderStatusAfterPartialAndFullReceipts() {
		TestOrder testOrder = createSentOrder("receipt-state");
		long initialReceiptCount = goodsReceiptRepository.count();

		goodsReceiptService.create(
				testOrder.orderId(),
				testOrder.buyerId(),
				createCommand(testOrder.lineId(), "6.000")
		);

		PurchaseOrder partiallyReceived = purchaseOrderRepository.findById(testOrder.orderId()).orElseThrow();
		assertEquals(PurchaseOrderStatus.PARTIALLY_RECEIVED, partiallyReceived.getStatus());
		assertEquals(new BigDecimal("6.000"), getPostedQuantity(testOrder.lineId()));

		goodsReceiptService.create(
				testOrder.orderId(),
				testOrder.buyerId(),
				createCommand(testOrder.lineId(), "4.000")
		);

		PurchaseOrder received = purchaseOrderRepository.findById(testOrder.orderId()).orElseThrow();
		assertEquals(PurchaseOrderStatus.RECEIVED, received.getStatus());
		assertEquals(new BigDecimal("10.000"), getPostedQuantity(testOrder.lineId()));
		assertEquals(initialReceiptCount + 2, goodsReceiptRepository.count());
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
		return new CreateGoodsReceiptCommand(
				LocalDate.now(),
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
