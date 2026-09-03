package com.dayoung.procurement;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class ClosePeriodMigrationTest {

	private final JdbcClient jdbcClient;

	@Autowired
	ClosePeriodMigrationTest(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

	@Test
	void createsClosePeriodTableWithFlywayV8() {
		Integer tableCount = jdbcClient.sql("""
				SELECT COUNT(*)
				FROM information_schema.tables
				WHERE table_schema = DATABASE()
				  AND table_name = 'close_period'
				""")
				.query(Integer.class)
				.single();
		Integer migrationCount = jdbcClient.sql("""
				SELECT COUNT(*)
				FROM flyway_schema_history
				WHERE version = '8'
				  AND script = 'V8__create_close_period_table.sql'
				  AND success = TRUE
				""")
				.query(Integer.class)
				.single();

		assertEquals(1, tableCount);
		assertEquals(1, migrationCount);
	}
}
