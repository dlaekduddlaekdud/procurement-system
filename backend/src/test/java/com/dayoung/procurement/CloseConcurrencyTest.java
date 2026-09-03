package com.dayoung.procurement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dayoung.procurement.closing.application.CloseService;
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
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class CloseConcurrencyTest {

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

	@Test
	void createsOnlyOneSuccessfulResultForConcurrentManualAndScheduledClosing() throws Exception {
		AppUser admin = createAdmin();
		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(2);

		try {
			Future<Long> manualRun = executor.submit(() -> executeTogether(
					ready,
					start,
					() -> closeService.closeManually("202607", admin.getId())
			));
			Future<Long> scheduledRun = executor.submit(() -> executeTogether(
					ready,
					start,
					() -> closeService.closeAutomatically("202607")
			));

			assertTrue(ready.await(5, TimeUnit.SECONDS));
			start.countDown();
			manualRun.get(10, TimeUnit.SECONDS);
			scheduledRun.get(10, TimeUnit.SECONDS);
		} finally {
			executor.shutdownNow();
		}

		var closePeriod = closePeriodRepository.findByPeriod("202607").orElseThrow();
		List<CloseRun> runs = closeRunRepository
				.findAllByClosePeriod_IdOrderByAttemptNo(closePeriod.getId());
		assertEquals(ClosePeriodStatus.CLOSED, closePeriod.getStatus());
		assertEquals(2, runs.size());
		assertEquals(List.of(1, 2), runs.stream().map(CloseRun::getAttemptNo).toList());
		assertEquals(1, runs.stream().filter(run -> run.getStatus() == CloseRunStatus.SUCCESS).count());
		assertEquals(1, runs.stream().filter(run -> run.getStatus() == CloseRunStatus.SKIPPED).count());
		assertEquals(1, runs.stream().filter(run -> run.getTriggerType() == CloseRunTriggerType.MANUAL).count());
		assertEquals(1, runs.stream().filter(run -> run.getTriggerType() == CloseRunTriggerType.SCHEDULED).count());
		assertNull(runs.stream()
				.filter(run -> run.getTriggerType() == CloseRunTriggerType.SCHEDULED)
				.findFirst()
				.orElseThrow()
				.getRequestedBy());
	}

	private Long executeTogether(
			CountDownLatch ready,
			CountDownLatch start,
			CloseOperation operation
	) throws Exception {
		ready.countDown();
		if (!start.await(5, TimeUnit.SECONDS)) {
			throw new IllegalStateException("동시 마감 시작 신호를 기다리지 못했습니다.");
		}
		return operation.execute();
	}

	private AppUser createAdmin() {
		Department department = departmentRepository.findByCode("FINANCE").orElseThrow();
		String suffix = Long.toUnsignedString(System.nanoTime(), 36);
		AppUser admin = appUserRepository.save(new AppUser(
				"concurrent-close-admin-" + suffix + "@example.com",
				"encoded-password",
				"동시 마감 관리자",
				department
		));
		Role role = roleRepository.findByCode(RoleCode.ADMIN).orElseThrow();
		userRoleRepository.save(new UserRole(admin, role));
		return admin;
	}

	@FunctionalInterface
	private interface CloseOperation {

		Long execute();
	}
}
