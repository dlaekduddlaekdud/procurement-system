package com.dayoung.procurement;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dayoung.procurement.closing.application.CloseService;
import com.dayoung.procurement.closing.domain.CloseAccrualSnapshot;
import com.dayoung.procurement.closing.domain.CloseHoldSnapshot;
import com.dayoung.procurement.closing.repository.CloseAccrualSnapshotRepository;
import com.dayoung.procurement.closing.repository.CloseHoldSnapshotRepository;
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
import com.dayoung.procurement.matching.domain.MatchingStatus;
import com.dayoung.procurement.purchase.application.CreatePurchaseOrderCommand;
import com.dayoung.procurement.purchase.application.CreatePurchaseRequestCommand;
import com.dayoung.procurement.purchase.application.CreatePurchaseRequestLineCommand;
import com.dayoung.procurement.purchase.application.PurchaseOrderService;
import com.dayoung.procurement.purchase.application.PurchaseRequestService;
import com.dayoung.procurement.purchase.domain.PurchaseOrderLine;
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
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
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
class CloseSnapshotServiceTest {

	@Autowired
	private CloseService closeService;

	@Autowired
	private PurchaseRequestService purchaseRequestService;

	@Autowired
	private PurchaseOrderService purchaseOrderService;

	@Autowired
	private GoodsReceiptService goodsReceiptService;

	@Autowired
	private InvoiceService invoiceService;

	@Autowired
	private CloseAccrualSnapshotRepository accrualSnapshotRepository;

	@Autowired
	private CloseHoldSnapshotRepository holdSnapshotRepository;

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
	void savesOutstandingAccrualAndUnresolvedHoldAtClosing() {
		String suffix = Long.toUnsignedString(System.nanoTime(), 36);
		AppUser requester = createUser("snapshot-requester-" + suffix + "@example.com", RoleCode.REQUESTER);
		AppUser buyer = createUser("snapshot-buyer-" + suffix + "@example.com", RoleCode.BUYER);
		AppUser admin = createUser("snapshot-admin-" + suffix + "@example.com", RoleCode.ADMIN);
		Item item = itemRepository.save(new Item(
				"ITEM-CLOSE-" + suffix,
				"마감 스냅샷 품목",
				null,
				"EA",
				new BigDecimal("1000.00")
		));
		Vendor vendor = vendorRepository.save(new Vendor(
				"V-CLOSE-" + suffix,
				"마감 스냅샷 공급업체",
				"BIZ-" + suffix
		));
		Warehouse warehouse = warehouseRepository.findByCode("WH-SEOUL").orElseThrow();
		LocalDate postingDate = LocalDate.now();

		Long requestId = purchaseRequestService.create(
				requester.getId(),
				new CreatePurchaseRequestCommand(
						"마감 스냅샷 구매요청",
						null,
						postingDate,
						List.of(new CreatePurchaseRequestLineCommand(
								item.getId(),
								BigDecimal.ONE,
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
				new CreatePurchaseOrderCommand(vendor.getId(), warehouse.getId(), postingDate)
		);
		purchaseOrderService.send(orderId, buyer.getId());
		PurchaseOrderLine orderLine = purchaseOrderLineRepository
				.findAllByPurchaseOrder_IdOrderByLineNumber(orderId)
				.getFirst();
		goodsReceiptService.create(
				orderId,
				buyer.getId(),
				new CreateGoodsReceiptCommand(
						postingDate,
						List.of(new CreateGoodsReceiptLineCommand(orderLine.getId(), new BigDecimal("0.800")))
				)
		);
		invoiceService.create(
				orderId,
				buyer.getId(),
				new CreateInvoiceCommand(
						"INV-CLOSE-" + suffix,
						postingDate,
						postingDate,
						List.of(new CreateInvoiceLineCommand(
								orderLine.getId(),
								BigDecimal.ONE,
								new BigDecimal("1000.00")
						))
				)
		);

		String period = postingDate.format(DateTimeFormatter.ofPattern("yyyyMM"));
		Long closeRunId = closeService.closeManually(period, admin.getId());
		entityManager.flush();
		entityManager.clear();

		List<CloseAccrualSnapshot> accrualSnapshots = accrualSnapshotRepository
				.findAllByCloseRun_IdOrderByPurchaseOrderLine_Id(closeRunId);
		List<CloseHoldSnapshot> holdSnapshots = holdSnapshotRepository
				.findAllByCloseRun_IdOrderByPurchaseOrderLine_Id(closeRunId);
		assertEquals(1, accrualSnapshots.size());
		assertEquals(orderLine.getId(), accrualSnapshots.getFirst().getPurchaseOrderLine().getId());
		assertEquals(new BigDecimal("800.00"), accrualSnapshots.getFirst().getBalanceAmount());
		assertEquals("KRW", accrualSnapshots.getFirst().getCurrency());
		assertEquals(1, holdSnapshots.size());
		assertEquals(MatchingStatus.HOLD_QUANTITY, holdSnapshots.getFirst().getStatus());
		assertEquals(new BigDecimal("0.800"), holdSnapshots.getFirst().getReceivedQuantity());
		assertEquals(new BigDecimal("1.000"), holdSnapshots.getFirst().getInvoicedQuantity());
		assertEquals(new BigDecimal("1000.00"), holdSnapshots.getFirst().getInvoicedAmount());
	}

	private AppUser createUser(String email, RoleCode roleCode) {
		Department department = departmentRepository.findByCode("FINANCE").orElseThrow();
		AppUser user = appUserRepository.save(new AppUser(email, "encoded-password", "마감 테스트", department));
		Role role = roleRepository.findByCode(roleCode).orElseThrow();
		userRoleRepository.save(new UserRole(user, role));
		return user;
	}
}
