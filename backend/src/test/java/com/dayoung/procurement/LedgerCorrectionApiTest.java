package com.dayoung.procurement;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dayoung.procurement.ledger.application.LedgerCorrectionService;
import com.dayoung.procurement.ledger.application.RepostAccrualEntryCommand;
import com.dayoung.procurement.ledger.application.ReverseAccrualEntryCommand;
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
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@SpringBootTest
@Transactional
class LedgerCorrectionApiTest {

	private static final String PASSWORD = "test-password";

	@Autowired
	private MockMvc mockMvc;
	@MockitoBean
	private LedgerCorrectionService ledgerCorrectionService;
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
	void reversesAndRepostsAccrualEntryAsAdmin() throws Exception {
		AppUser admin = createUser(RoleCode.ADMIN);
		entityManager.flush();
		when(ledgerCorrectionService.reverse(
				eq(10L),
				eq(admin.getId()),
				any(ReverseAccrualEntryCommand.class)
		)).thenReturn(11L);
		when(ledgerCorrectionService.repost(
				eq(11L),
				eq(admin.getId()),
				any(RepostAccrualEntryCommand.class)
		)).thenReturn(12L);

		mockMvc.perform(post("/api/accrual-entries/{entryId}/reverse", 10L)
						.with(httpBasic(admin.getEmail(), PASSWORD))
						.contentType(MediaType.APPLICATION_JSON)
						.content(reverseJson()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").value(11));

		mockMvc.perform(post("/api/accrual-entries/{reversalId}/repost", 11L)
						.with(httpBasic(admin.getEmail(), PASSWORD))
						.contentType(MediaType.APPLICATION_JSON)
						.content(repostJson("4000.00")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").value(12));
	}

	@Test
	void rejectsLedgerCorrectionByBuyer() throws Exception {
		AppUser buyer = createUser(RoleCode.BUYER);
		entityManager.flush();

		mockMvc.perform(post("/api/accrual-entries/{entryId}/reverse", 10L)
						.with(httpBasic(buyer.getEmail(), PASSWORD))
						.contentType(MediaType.APPLICATION_JSON)
						.content(reverseJson()))
				.andExpect(status().isForbidden());

		verifyNoInteractions(ledgerCorrectionService);
	}

	@Test
	void rejectsInvalidRepostingAmount() throws Exception {
		AppUser admin = createUser(RoleCode.ADMIN);
		entityManager.flush();

		mockMvc.perform(post("/api/accrual-entries/{reversalId}/repost", 11L)
						.with(httpBasic(admin.getEmail(), PASSWORD))
						.contentType(MediaType.APPLICATION_JSON)
						.content(repostJson("0")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
	}

	private String reverseJson() {
		return """
				{"postingDate":"%s","reason":"마감 후 금액 오류"}
				""".formatted(LocalDate.now());
	}

	private String repostJson(String amount) {
		return """
				{"amount":%s,"postingDate":"%s","reason":"정상 금액 재기표"}
				""".formatted(amount, LocalDate.now());
	}

	private AppUser createUser(RoleCode roleCode) {
		String suffix = Long.toUnsignedString(System.nanoTime(), 36);
		Department department = departmentRepository.findByCode("FINANCE").orElseThrow();
		AppUser user = appUserRepository.save(new AppUser(
				"ledger-api-" + roleCode.name().toLowerCase() + "-" + suffix + "@example.com",
				passwordEncoder.encode(PASSWORD),
				"원장 정정 API 테스트",
				department
		));
		Role role = roleRepository.findByCode(roleCode).orElseThrow();
		userRoleRepository.save(new UserRole(user, role));
		return user;
	}
}
