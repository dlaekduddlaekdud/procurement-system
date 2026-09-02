package com.dayoung.procurement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class UserRoleRepositoryTest {

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
	void savesUserAndAssignedRole() {
		Department department = departmentRepository.save(new Department("PURCHASE", "구매팀", null));
		AppUser user = appUserRepository.save(new AppUser(
				"buyer@example.com",
				"encoded-password",
				"구매 담당자",
				department
		));
		Role role = roleRepository.save(new Role(RoleCode.BUYER, "구매 담당자", "구매 승인 및 발주 담당"));
		userRoleRepository.save(new UserRole(user, role));

		entityManager.flush();
		entityManager.clear();

		AppUser savedUser = appUserRepository.findByEmail("buyer@example.com").orElseThrow();
		assertEquals("PURCHASE", savedUser.getDepartment().getCode());
		assertTrue(userRoleRepository.existsByAppUser_IdAndRole_Code(savedUser.getId(), RoleCode.BUYER));
		assertEquals(1, userRoleRepository.findAllByAppUser_Id(savedUser.getId()).size());
	}
}
