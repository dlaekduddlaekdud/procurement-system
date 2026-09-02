package com.dayoung.procurement.security;

import com.dayoung.procurement.user.domain.AppUser;
import com.dayoung.procurement.user.domain.RoleCode;
import com.dayoung.procurement.user.repository.AppUserRepository;
import com.dayoung.procurement.user.repository.UserRoleRepository;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProcurementUserDetailsService implements UserDetailsService {

	private final AppUserRepository appUserRepository;
	private final UserRoleRepository userRoleRepository;

	public ProcurementUserDetailsService(
			AppUserRepository appUserRepository,
			UserRoleRepository userRoleRepository
	) {
		this.appUserRepository = appUserRepository;
		this.userRoleRepository = userRoleRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		AppUser user = appUserRepository.findByEmail(email)
				.orElseThrow(() -> new UsernameNotFoundException("이메일 또는 비밀번호가 올바르지 않습니다."));
		Set<RoleCode> roles = userRoleRepository.findAllByAppUser_Id(user.getId()).stream()
				.map(userRole -> userRole.getRole().getCode())
				.collect(Collectors.toUnmodifiableSet());

		return new AuthenticatedUser(
				user.getId(),
				user.getEmail(),
				user.getPasswordHash(),
				roles,
				user.isActive()
		);
	}
}
