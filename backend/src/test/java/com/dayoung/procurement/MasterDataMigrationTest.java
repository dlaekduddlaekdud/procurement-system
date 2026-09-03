package com.dayoung.procurement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
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
class MasterDataMigrationTest {

	private final JdbcClient jdbcClient;

	@Autowired
	MasterDataMigrationTest(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

	@Test
	void createsAllMasterDataTables() {
		Integer tableCount = jdbcClient.sql("""
				SELECT COUNT(*)
				FROM information_schema.tables
				WHERE table_schema = DATABASE()
				  AND table_name IN ('department', 'warehouse', 'item', 'vendor')
				""")
				.query(Integer.class)
				.single();
		Integer migrationCount = jdbcClient.sql("""
				SELECT COUNT(*)
				FROM flyway_schema_history
				WHERE version = '1'
				  AND script = 'V1__create_master_data_tables.sql'
				  AND success = TRUE
				""")
				.query(Integer.class)
				.single();

		assertEquals(4, tableCount);
		assertEquals(1, migrationCount);
	}

	@Test
	void rejectsDuplicateMasterDataCodes() {
		insertDepartment("DEV", "개발팀");

		assertThrows(DataIntegrityViolationException.class,
				() -> insertDepartment("DEV", "플랫폼개발팀"));
	}

	@Test
	void rejectsNegativeStandardUnitPrice() {
		assertThrows(DataAccessException.class, () -> jdbcClient.sql("""
				INSERT INTO item (code, name, unit, standard_unit_price)
				VALUES (:code, :name, :unit, :standardUnitPrice)
				""")
				.param("code", "ITEM-001")
				.param("name", "노트북")
				.param("unit", "EA")
				.param("standardUnitPrice", new BigDecimal("-1.00"))
				.update());
	}

	private void insertDepartment(String code, String name) {
		jdbcClient.sql("""
				INSERT INTO department (code, name)
				VALUES (:code, :name)
				""")
				.param("code", code)
				.param("name", name)
				.update();
	}
}
