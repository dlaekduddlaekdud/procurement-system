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
class CloseRunMigrationTest {

	private final JdbcClient jdbcClient;

	@Autowired
	CloseRunMigrationTest(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

	@Test
	void createsCloseRunTableWithFlywayV11() {
		Integer tableCount = jdbcClient.sql("""
				SELECT COUNT(*)
				FROM information_schema.tables
				WHERE table_schema = DATABASE()
				  AND table_name = 'close_run'
				""")
				.query(Integer.class)
				.single();
		Integer migrationCount = jdbcClient.sql("""
				SELECT COUNT(*)
				FROM flyway_schema_history
				WHERE version = '11'
				  AND script = 'V11__create_close_run_table.sql'
				  AND success = TRUE
				""")
				.query(Integer.class)
				.single();

		assertEquals(1, tableCount);
		assertEquals(1, migrationCount);
	}
}
