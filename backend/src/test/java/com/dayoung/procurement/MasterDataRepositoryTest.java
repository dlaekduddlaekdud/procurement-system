package com.dayoung.procurement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dayoung.procurement.masterdata.domain.Department;
import com.dayoung.procurement.masterdata.domain.Item;
import com.dayoung.procurement.masterdata.domain.Vendor;
import com.dayoung.procurement.masterdata.domain.Warehouse;
import com.dayoung.procurement.masterdata.repository.DepartmentRepository;
import com.dayoung.procurement.masterdata.repository.ItemRepository;
import com.dayoung.procurement.masterdata.repository.VendorRepository;
import com.dayoung.procurement.masterdata.repository.WarehouseRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class MasterDataRepositoryTest {

	@Autowired
	private DepartmentRepository departmentRepository;

	@Autowired
	private WarehouseRepository warehouseRepository;

	@Autowired
	private ItemRepository itemRepository;

	@Autowired
	private VendorRepository vendorRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void savesAndFindsMasterDataByCode() {
		Department department = departmentRepository.save(new Department("DEV", "개발팀", "서비스 개발 담당"));
		Warehouse warehouse = warehouseRepository.save(new Warehouse("WH-TEST-SEOUL", "테스트 서울 창고", "서울특별시"));
		Item item = itemRepository.save(new Item(
				"ITEM-001",
				"업무용 노트북",
				"14인치, 32GB RAM",
				"EA",
				new BigDecimal("2500000.00")
		));
		Vendor vendor = vendorRepository.save(new Vendor("VENDOR-001", "테스트 공급업체", "1234567890"));

		entityManager.flush();
		entityManager.clear();

		assertNotNull(department.getId());
		assertNotNull(warehouse.getId());
		assertNotNull(item.getId());
		assertNotNull(vendor.getId());
		assertEquals("개발팀", departmentRepository.findByCode("DEV").orElseThrow().getName());
		assertEquals("테스트 서울 창고", warehouseRepository.findByCode("WH-TEST-SEOUL").orElseThrow().getName());
		assertEquals(new BigDecimal("2500000.00"),
				itemRepository.findByCode("ITEM-001").orElseThrow().getStandardUnitPrice());
		assertTrue(vendorRepository.existsByBusinessRegistrationNumber("1234567890"));
	}
}
