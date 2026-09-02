package com.dayoung.procurement.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_role")
public class UserRole {

	@EmbeddedId
	private UserRoleId id;

	@MapsId("appUserId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "app_user_id", nullable = false)
	private AppUser appUser;

	@MapsId("roleId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "role_id", nullable = false)
	private Role role;

	@Column(name = "assigned_at", nullable = false, insertable = false, updatable = false)
	private LocalDateTime assignedAt;

	protected UserRole() {
	}

	public UserRole(AppUser appUser, Role role) {
		this.appUser = appUser;
		this.role = role;
		this.id = new UserRoleId(appUser.getId(), role.getId());
	}

	public UserRoleId getId() {
		return id;
	}

	public AppUser getAppUser() {
		return appUser;
	}

	public Role getRole() {
		return role;
	}

	public LocalDateTime getAssignedAt() {
		return assignedAt;
	}
}
