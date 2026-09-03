package com.dayoung.procurement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dayoung.procurement.closing.domain.ClosePeriodStatus;
import com.dayoung.procurement.closing.domain.CloseRun;
import com.dayoung.procurement.closing.domain.CloseRunStatus;
import com.dayoung.procurement.closing.repository.ClosePeriodRepository;
import com.dayoung.procurement.closing.repository.CloseRunRepository;
import com.dayoung.procurement.masterdata.domain.Department;
import com.dayoung.procurement.masterdata.repository.DepartmentRepository;
import com.dayoung.procurement.user.domain.AppUser;
import com.dayoung.procurement.user.domain.Role;
import com.dayoung.procurement.user.domain.RoleCode;
import com.dayoung.procurement.user.domain.UserRole;
import com.dayoung.procurement.user.repository.AppUserRepository;
import com.dayoung.procurement.user.repository.RoleRepository;
import com.dayoung.procurement.user.repository.UserRoleRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@SpringBootTest
@Transactional
class ClosePeriodApiTest {

	private static final String PASSWORD = "test-password";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private ClosePeriodRepository closePeriodRepository;

	@Autowired
	private CloseRunRepository closeRunRepository;

	@Autowired
	private DepartmentRepository departmentRepository;

	@Autowired
	private AppUserRepository appUserRepository;

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private UserRoleRepository userRoleRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void closesPeriodAsAdmin() throws Exception {
		AppUser admin = createUser(RoleCode.ADMIN);
		entityManager.flush();

		mockMvc.perform(post("/api/close-periods/{period}/close", "202611")
					.with(httpBasic(admin.getEmail(), PASSWORD)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").isNumber());

		entityManager.flush();
		entityManager.clear();
		var closePeriod = closePeriodRepository.findByPeriod("202611").orElseThrow();
		CloseRun closeRun = closeRunRepository
				.findAllByClosePeriod_IdOrderByAttemptNo(closePeriod.getId())
				.getFirst();
		assertEquals(ClosePeriodStatus.CLOSED, closePeriod.getStatus());
		assertEquals(CloseRunStatus.SUCCESS, closeRun.getStatus());
		assertEquals(admin.getId(), closeRun.getRequestedBy().getId());
	}

	@Test
	void rejectsManualClosingByBuyer() throws Exception {
		AppUser buyer = createUser(RoleCode.BUYER);
		entityManager.flush();

		mockMvc.perform(post("/api/close-periods/{period}/close", "202612")
					.with(httpBasic(buyer.getEmail(), PASSWORD)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
	}

	@Test
	void rejectsInvalidClosePeriodFormat() throws Exception {
		AppUser admin = createUser(RoleCode.ADMIN);
		entityManager.flush();

		mockMvc.perform(post("/api/close-periods/{period}/close", "2026-11")
					.with(httpBasic(admin.getEmail(), PASSWORD)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
	}

	private AppUser createUser(RoleCode roleCode) {
		String suffix = Long.toUnsignedString(System.nanoTime(), 36);
		Department department = departmentRepository.findByCode("FINANCE").orElseThrow();
		AppUser user = appUserRepository.save(new AppUser(
				"close-api-" + roleCode.name().toLowerCase() + "-" + suffix + "@example.com",
				passwordEncoder.encode(PASSWORD),
				"마감 API 테스트",
				department
		));
		Role role = roleRepository.findByCode(roleCode).orElseThrow();
		userRoleRepository.save(new UserRole(user, role));
		return user;
	}
}
