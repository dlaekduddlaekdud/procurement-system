package com.dayoung.procurement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.dayoung.procurement.closing.application.CloseService;
import com.dayoung.procurement.closing.domain.ClosePeriod;
import com.dayoung.procurement.closing.domain.ClosePeriodStatus;
import com.dayoung.procurement.closing.domain.CloseRun;
import com.dayoung.procurement.closing.domain.CloseRunStatus;
import com.dayoung.procurement.closing.domain.CloseRunTriggerType;
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
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class CloseServiceTest {

	@Autowired
	private CloseService closeService;

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
	void closesOpenPeriodAndSkipsRepeatedExecution() {
		AppUser admin = createAdmin();

		Long successRunId = closeService.closeManually("202609", admin.getId());
		entityManager.flush();
		entityManager.clear();

		ClosePeriod closedPeriod = closePeriodRepository.findByPeriod("202609").orElseThrow();
		LocalDateTime firstClosedAt = closedPeriod.getClosedAt();
		CloseRun successRun = closeRunRepository.findById(successRunId).orElseThrow();
		assertEquals(ClosePeriodStatus.CLOSED, closedPeriod.getStatus());
		assertNotNull(firstClosedAt);
		assertEquals(1, successRun.getAttemptNo());
		assertEquals(CloseRunStatus.SUCCESS, successRun.getStatus());
		assertEquals(CloseRunTriggerType.MANUAL, successRun.getTriggerType());
		assertEquals(admin.getId(), successRun.getRequestedBy().getId());

		Long skippedRunId = closeService.closeManually("202609", admin.getId());
		entityManager.flush();
		entityManager.clear();

		ClosePeriod unchangedPeriod = closePeriodRepository.findByPeriod("202609").orElseThrow();
		CloseRun skippedRun = closeRunRepository.findById(skippedRunId).orElseThrow();
		List<CloseRun> runs = closeRunRepository
				.findAllByClosePeriod_IdOrderByAttemptNo(unchangedPeriod.getId());
		assertEquals(ClosePeriodStatus.CLOSED, unchangedPeriod.getStatus());
		assertEquals(firstClosedAt, unchangedPeriod.getClosedAt());
		assertEquals(2, skippedRun.getAttemptNo());
		assertEquals(CloseRunStatus.SKIPPED, skippedRun.getStatus());
		assertEquals(List.of(CloseRunStatus.SUCCESS, CloseRunStatus.SKIPPED),
				runs.stream().map(CloseRun::getStatus).toList());
	}

	private AppUser createAdmin() {
		Department department = departmentRepository.findByCode("FINANCE").orElseThrow();
		String suffix = Long.toUnsignedString(System.nanoTime(), 36);
		AppUser admin = appUserRepository.save(new AppUser(
				"close-admin-" + suffix + "@example.com",
				"encoded-password",
				"마감 관리자",
				department
		));
		Role role = roleRepository.findByCode(RoleCode.ADMIN).orElseThrow();
		userRoleRepository.save(new UserRole(admin, role));
		return admin;
	}
}
