package com.dayoung.procurement;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class InitialReferenceDataMigrationTest {

	private final JdbcClient jdbcClient;

	@Autowired
	InitialReferenceDataMigrationTest(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

	@Test
	void seedsDepartmentsWarehousesAndRolesWithFlywayV3() {
		Integer departmentCount = jdbcClient.sql("""
				SELECT COUNT(*) FROM department
				WHERE code IN ('PURCHASE', 'FINANCE', 'IT')
				""")
				.query(Integer.class)
				.single();
		Integer warehouseCount = jdbcClient.sql("""
				SELECT COUNT(*) FROM warehouse
				WHERE code IN ('WH-SEOUL', 'WH-BUSAN')
				""")
				.query(Integer.class)
				.single();
		Integer roleCount = jdbcClient.sql("""
				SELECT COUNT(*) FROM `role`
				WHERE code IN ('REQUESTER', 'BUYER', 'ADMIN')
				""")
				.query(Integer.class)
				.single();

		Integer migrationCount = jdbcClient.sql("""
				SELECT COUNT(*)
				FROM flyway_schema_history
				WHERE version = '3'
				  AND script = 'V3__seed_initial_reference_data.sql'
				  AND success = TRUE
				""")
				.query(Integer.class)
				.single();

		assertEquals(3, departmentCount);
		assertEquals(2, warehouseCount);
		assertEquals(3, roleCount);
		assertEquals(1, migrationCount);
	}
}
