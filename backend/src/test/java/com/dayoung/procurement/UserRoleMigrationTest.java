package com.dayoung.procurement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class UserRoleMigrationTest {

	private final JdbcClient jdbcClient;

	@Autowired
	UserRoleMigrationTest(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

	@Test
	void createsUserAndRoleTablesWithFlywayV2() {
		Integer tableCount = jdbcClient.sql("""
				SELECT COUNT(*)
				FROM information_schema.tables
				WHERE table_schema = DATABASE()
				  AND table_name IN ('app_user', 'role', 'user_role')
				""")
				.query(Integer.class)
				.single();
		Integer migrationCount = jdbcClient.sql("""
				SELECT COUNT(*)
				FROM flyway_schema_history
				WHERE version = '2'
				  AND script = 'V2__create_user_and_role_tables.sql'
				  AND success = TRUE
				""")
				.query(Integer.class)
				.single();

		assertEquals(3, tableCount);
		assertEquals(1, migrationCount);
	}

	@Test
	void rejectsUserWithUnknownDepartment() {
		assertThrows(DataIntegrityViolationException.class, () -> jdbcClient.sql("""
				INSERT INTO app_user (email, password_hash, name, department_id)
				VALUES (:email, :passwordHash, :name, :departmentId)
				""")
				.param("email", "requester@example.com")
				.param("passwordHash", "encoded-password")
				.param("name", "구매 요청자")
				.param("departmentId", Long.MAX_VALUE)
				.update());
	}

	@Test
	void rejectsDuplicateRoleAssignment() {
		Long departmentId = findDepartmentId();
		Long userId = insertUser(departmentId);
		Long roleId = findRoleId();
		assignRole(userId, roleId);

		assertThrows(DataIntegrityViolationException.class, () -> assignRole(userId, roleId));
	}

	private Long findDepartmentId() {
		return jdbcClient.sql("SELECT id FROM department WHERE code = 'PURCHASE'")
				.query(Long.class)
				.single();
	}

	private Long insertUser(Long departmentId) {
		jdbcClient.sql("""
				INSERT INTO app_user (email, password_hash, name, department_id)
				VALUES ('buyer@example.com', 'encoded-password', '구매 담당자', :departmentId)
				""")
				.param("departmentId", departmentId)
				.update();

		return jdbcClient.sql("SELECT id FROM app_user WHERE email = 'buyer@example.com'")
				.query(Long.class)
				.single();
	}

	private Long findRoleId() {
		return jdbcClient.sql("SELECT id FROM `role` WHERE code = 'BUYER'")
				.query(Long.class)
				.single();
	}

	private void assignRole(Long userId, Long roleId) {
		jdbcClient.sql("""
				INSERT INTO user_role (app_user_id, role_id)
				VALUES (:userId, :roleId)
				""")
				.param("userId", userId)
				.param("roleId", roleId)
				.update();
	}
}
