package com.dayoung.procurement;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class PurchaseRequestDecisionMigrationTest {

	private final JdbcClient jdbcClient;

	@Autowired
	PurchaseRequestDecisionMigrationTest(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

	@Test
	void addsRejectionAuditColumnsWithFlywayV5() {
		Integer columnCount = jdbcClient.sql("""
				SELECT COUNT(*)
				FROM information_schema.columns
				WHERE table_schema = DATABASE()
				  AND table_name = 'purchase_request'
				  AND column_name IN ('rejected_by', 'rejected_at')
				""")
				.query(Integer.class)
				.single();
		Integer migrationCount = jdbcClient.sql("""
				SELECT COUNT(*)
				FROM flyway_schema_history
				WHERE version = '5'
				  AND script = 'V5__add_purchase_request_rejection_audit.sql'
				  AND success = TRUE
				""")
				.query(Integer.class)
				.single();

		assertEquals(2, columnCount);
		assertEquals(1, migrationCount);
	}
}
