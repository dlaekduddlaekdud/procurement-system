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
class AccrualRepostingMigrationTest {

	private final JdbcClient jdbcClient;

	@Autowired
	AccrualRepostingMigrationTest(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

	@Test
	void addsRepostingLinkWithFlywayV14() {
		Integer columnCount = jdbcClient.sql("""
				SELECT COUNT(*)
				FROM information_schema.columns
				WHERE table_schema = DATABASE()
				  AND table_name = 'accrual_entry'
				  AND column_name = 'reposting_of_id'
				""")
				.query(Integer.class)
				.single();
		Integer migrationCount = jdbcClient.sql("""
				SELECT COUNT(*)
				FROM flyway_schema_history
				WHERE version = '14'
				  AND script = 'V14__add_accrual_reposting_support.sql'
				  AND success = TRUE
				""")
				.query(Integer.class)
				.single();

		assertEquals(1, columnCount);
		assertEquals(1, migrationCount);
	}
}
