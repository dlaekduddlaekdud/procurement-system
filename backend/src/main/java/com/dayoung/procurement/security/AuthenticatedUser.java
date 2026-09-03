package com.dayoung.procurement.security;

import com.dayoung.procurement.user.domain.RoleCode;
import java.io.Serial;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class AuthenticatedUser implements UserDetails, CredentialsContainer {

	@Serial
	private static final long serialVersionUID = 1L;

	private final Long userId;
	private final String email;
	private final Set<RoleCode> roles;
	private final boolean active;
	private String password;

	public AuthenticatedUser(Long userId, String email, String password, Set<RoleCode> roles, boolean active) {
		this.userId = userId;
		this.email = email;
		this.password = password;
		this.roles = Set.copyOf(roles);
		this.active = active;
	}

	public Long getUserId() {
		return userId;
	}

	public Set<RoleCode> getRoles() {
		return roles;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return roles.stream()
				.map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
				.collect(Collectors.toUnmodifiableSet());
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public String getUsername() {
		return email;
	}

	@Override
	public boolean isEnabled() {
		return active;
	}

	@Override
	public void eraseCredentials() {
		password = null;
	}
}
