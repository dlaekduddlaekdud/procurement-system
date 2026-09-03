package com.dayoung.procurement.security;

import com.dayoung.procurement.user.domain.RoleCode;
import java.util.Set;

public record CurrentUserResponse(
		Long id,
		String email,
		Set<RoleCode> roles
) {
	public static CurrentUserResponse from(AuthenticatedUser user) {
		return new CurrentUserResponse(user.getUserId(), user.getUsername(), user.getRoles());
	}
}
