package com.dayoung.procurement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class PurchaseDocumentMigrationTest {

	private final JdbcClient jdbcClient;

	@Autowired
	PurchaseDocumentMigrationTest(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

	@Test
	void createsPurchaseRequestAndOrderTablesWithFlywayV4() {
		Integer tableCount = jdbcClient.sql("""
				SELECT COUNT(*)
				FROM information_schema.tables
				WHERE table_schema = DATABASE()
				  AND table_name IN (
				      'purchase_request', 'purchase_request_line',
				      'purchase_order', 'purchase_order_line'
				  )
				""")
				.query(Integer.class)
				.single();
		Integer migrationCount = jdbcClient.sql("""
				SELECT COUNT(*)
				FROM flyway_schema_history
				WHERE version = '4'
				  AND script = 'V4__create_purchase_request_and_order_tables.sql'
				  AND success = TRUE
				""")
				.query(Integer.class)
				.single();

		assertEquals(4, tableCount);
		assertEquals(1, migrationCount);
	}

	@Test
	void rejectsNonPositivePurchaseRequestLineQuantity() {
		Long requesterId = insertUser("requester-test@example.com");
		Long requestId = insertPurchaseRequest("PR-TEST-001", requesterId);
		Long itemId = insertItem();

		assertThrows(DataAccessException.class, () -> jdbcClient.sql("""
				INSERT INTO purchase_request_line (
				    purchase_request_id, line_number, item_id, quantity, unit,
				    estimated_unit_price, estimated_amount
				)
				VALUES (:requestId, 1, :itemId, 0, 'EA', 1000.00, 0.00)
				""")
				.param("requestId", requestId)
				.param("itemId", itemId)
				.update());
	}

	@Test
	void rejectsDuplicatePurchaseOrderForSameRequest() {
		Long buyerId = insertUser("buyer-test@example.com");
		Long requestId = insertPurchaseRequest("PR-TEST-002", buyerId);
		Long vendorId = insertVendor();
		Long warehouseId = findWarehouseId();
		insertPurchaseOrder("PO-TEST-001", requestId, vendorId, buyerId, warehouseId);

		assertThrows(DataIntegrityViolationException.class,
				() -> insertPurchaseOrder("PO-TEST-002", requestId, vendorId, buyerId, warehouseId));
	}

	private Long insertUser(String email) {
		Long departmentId = findDepartmentId();
		jdbcClient.sql("""
				INSERT INTO app_user (email, password_hash, name, department_id)
				VALUES (:email, 'encoded-password', '테스트 사용자', :departmentId)
				""")
				.param("email", email)
				.param("departmentId", departmentId)
				.update();

		return jdbcClient.sql("SELECT id FROM app_user WHERE email = :email")
				.param("email", email)
				.query(Long.class)
				.single();
	}

	private Long insertPurchaseRequest(String requestNumber, Long requesterId) {
		Long departmentId = findDepartmentId();
		jdbcClient.sql("""
				INSERT INTO purchase_request (
				    request_number, requester_id, department_id, title, status, request_date
				)
				VALUES (:requestNumber, :requesterId, :departmentId, '테스트 구매요청', 'APPROVED', '2026-09-02')
				""")
				.param("requestNumber", requestNumber)
				.param("requesterId", requesterId)
				.param("departmentId", departmentId)
				.update();

		return jdbcClient.sql("SELECT id FROM purchase_request WHERE request_number = :requestNumber")
				.param("requestNumber", requestNumber)
				.query(Long.class)
				.single();
	}

	private Long insertItem() {
		jdbcClient.sql("""
				INSERT INTO item (code, name, unit, standard_unit_price)
				VALUES ('ITEM-PR-TEST', '테스트 품목', 'EA', 1000.00)
				""")
				.update();

		return jdbcClient.sql("SELECT id FROM item WHERE code = 'ITEM-PR-TEST'")
				.query(Long.class)
				.single();
	}

	private Long insertVendor() {
		jdbcClient.sql("""
				INSERT INTO vendor (code, name, business_registration_number)
				VALUES ('VENDOR-PO-TEST', '테스트 공급업체', '9999999999')
				""")
				.update();

		return jdbcClient.sql("SELECT id FROM vendor WHERE code = 'VENDOR-PO-TEST'")
				.query(Long.class)
				.single();
	}

	private Long findDepartmentId() {
		return jdbcClient.sql("SELECT id FROM department WHERE code = 'PURCHASE'")
				.query(Long.class)
				.single();
	}

	private Long findWarehouseId() {
		return jdbcClient.sql("SELECT id FROM warehouse WHERE code = 'WH-SEOUL'")
				.query(Long.class)
				.single();
	}

	private void insertPurchaseOrder(
			String orderNumber,
			Long requestId,
			Long vendorId,
			Long buyerId,
			Long warehouseId
	) {
		jdbcClient.sql("""
				INSERT INTO purchase_order (
				    order_number, purchase_request_id, vendor_id, buyer_id,
				    warehouse_id, status, order_date
				)
				VALUES (
				    :orderNumber, :requestId, :vendorId, :buyerId,
				    :warehouseId, 'CREATED', '2026-09-02'
				)
				""")
				.param("orderNumber", orderNumber)
				.param("requestId", requestId)
				.param("vendorId", vendorId)
				.param("buyerId", buyerId)
				.param("warehouseId", warehouseId)
				.update();
	}
}
