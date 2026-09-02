package com.dayoung.procurement;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class SecurityAuthenticationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PasswordEncoder passwordEncoder;

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
	void authenticatesDatabaseUserAndReturnsCurrentUser() throws Exception {
		String email = "security-requester@example.com";
		String password = "test-password";
		AppUser user = createUser(email, password, RoleCode.REQUESTER);
		entityManager.flush();

		mockMvc.perform(get("/api/auth/me").with(httpBasic(email, password)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").value(user.getId()))
				.andExpect(jsonPath("$.data.email").value(email))
				.andExpect(jsonPath("$.data.roles[0]").value("REQUESTER"))
				.andExpect(jsonPath("$.error").doesNotExist());
	}

	@Test
	void rejectsInvalidPasswordWithCommonErrorResponse() throws Exception {
		String email = "security-invalid-password@example.com";
		createUser(email, "correct-password", RoleCode.REQUESTER);
		entityManager.flush();

		mockMvc.perform(get("/api/auth/me").with(httpBasic(email, "wrong-password")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.data").doesNotExist())
				.andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
	}

	@Test
	void allowsHealthCheckWithoutAuthentication() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk());
	}

	private AppUser createUser(String email, String rawPassword, RoleCode roleCode) {
		Department department = departmentRepository.findByCode("IT").orElseThrow();
		AppUser user = appUserRepository.save(new AppUser(
				email,
				passwordEncoder.encode(rawPassword),
				"인증 테스트 사용자",
				department
		));
		Role role = roleRepository.findByCode(roleCode).orElseThrow();
		userRoleRepository.save(new UserRole(user, role));
		return user;
	}
}
