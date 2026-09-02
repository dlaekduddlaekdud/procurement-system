package com.dayoung.procurement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dayoung.procurement.masterdata.domain.Department;
import com.dayoung.procurement.masterdata.domain.Item;
import com.dayoung.procurement.masterdata.domain.Vendor;
import com.dayoung.procurement.masterdata.domain.Warehouse;
import com.dayoung.procurement.masterdata.repository.DepartmentRepository;
import com.dayoung.procurement.masterdata.repository.ItemRepository;
import com.dayoung.procurement.masterdata.repository.VendorRepository;
import com.dayoung.procurement.masterdata.repository.WarehouseRepository;
import com.dayoung.procurement.purchase.domain.PurchaseOrder;
import com.dayoung.procurement.purchase.domain.PurchaseRequest;
import com.dayoung.procurement.purchase.domain.PurchaseRequestLine;
import com.dayoung.procurement.purchase.repository.PurchaseOrderLineRepository;
import com.dayoung.procurement.purchase.repository.PurchaseOrderRepository;
import com.dayoung.procurement.purchase.repository.PurchaseRequestRepository;
import com.dayoung.procurement.user.domain.AppUser;
import com.dayoung.procurement.user.repository.AppUserRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class PurchaseDocumentRepositoryTest {

	@Autowired
	private DepartmentRepository departmentRepository;

	@Autowired
	private WarehouseRepository warehouseRepository;

	@Autowired
	private ItemRepository itemRepository;

	@Autowired
	private VendorRepository vendorRepository;

	@Autowired
	private AppUserRepository appUserRepository;

	@Autowired
	private PurchaseRequestRepository purchaseRequestRepository;

	@Autowired
	private PurchaseOrderRepository purchaseOrderRepository;

	@Autowired
	private PurchaseOrderLineRepository purchaseOrderLineRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void savesPurchaseRequestAndOrderWithLineLinkage() {
		Department department = departmentRepository.findByCode("IT").orElseThrow();
		Warehouse warehouse = warehouseRepository.findByCode("WH-SEOUL").orElseThrow();
		Item item = itemRepository.save(new Item(
				"ITEM-DOCUMENT-TEST",
				"업무용 노트북",
				"14인치, 32GB RAM",
				"EA",
				new BigDecimal("2500000.00")
		));
		Vendor vendor = vendorRepository.save(new Vendor(
				"VENDOR-DOCUMENT-TEST",
				"테스트 공급업체",
				"8888888888"
		));
		AppUser user = appUserRepository.save(new AppUser(
				"document-test@example.com",
				"encoded-password",
				"문서 테스트 사용자",
				department
		));
		PurchaseRequest request = new PurchaseRequest(
				"PR-DOCUMENT-TEST",
				user,
				department,
				"개발 장비 구매",
				"신규 입사자 장비 지급",
				LocalDate.of(2026, 9, 2),
				LocalDate.of(2026, 9, 15)
		);
		PurchaseRequestLine requestLine = request.addLine(
				item,
				new BigDecimal("10.000"),
				"EA",
				new BigDecimal("2500000.00"),
				new BigDecimal("25000000.00"),
				"개발자용 노트북"
		);
		purchaseRequestRepository.save(request);

		PurchaseOrder order = new PurchaseOrder(
				"PO-DOCUMENT-TEST",
				request,
				vendor,
				user,
				warehouse,
				LocalDate.of(2026, 9, 3),
				LocalDate.of(2026, 9, 15)
		);
		order.addLine(
				requestLine,
				item,
				new BigDecimal("10.000"),
				"EA",
				new BigDecimal("2500000.00"),
				new BigDecimal("25000000.00"),
				new BigDecimal("2500000.00"),
				new BigDecimal("27500000.00"),
				LocalDate.of(2026, 9, 15)
		);
		purchaseOrderRepository.save(order);

		entityManager.flush();
		entityManager.clear();

		PurchaseRequest savedRequest = purchaseRequestRepository.findByRequestNumber("PR-DOCUMENT-TEST")
				.orElseThrow();
		PurchaseOrder savedOrder = purchaseOrderRepository.findByOrderNumber("PO-DOCUMENT-TEST")
				.orElseThrow();

		assertEquals(1, savedRequest.getLines().size());
		assertEquals("ITEM-DOCUMENT-TEST", savedRequest.getLines().getFirst().getItem().getCode());
		assertEquals(savedRequest.getId(), savedOrder.getPurchaseRequest().getId());
		assertEquals(1, savedOrder.getLines().size());
		assertEquals(savedRequest.getLines().getFirst().getId(),
				savedOrder.getLines().getFirst().getPurchaseRequestLine().getId());
		assertTrue(purchaseOrderLineRepository.existsByPurchaseRequestLine_Id(
				savedRequest.getLines().getFirst().getId()
		));
	}
}
