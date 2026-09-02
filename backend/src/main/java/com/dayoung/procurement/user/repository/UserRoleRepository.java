package com.dayoung.procurement.user.repository;

import com.dayoung.procurement.user.domain.RoleCode;
import com.dayoung.procurement.user.domain.UserRole;
import com.dayoung.procurement.user.domain.UserRoleId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {

	List<UserRole> findAllByAppUser_Id(Long appUserId);

	boolean existsByAppUser_IdAndRole_Code(Long appUserId, RoleCode roleCode);
}
