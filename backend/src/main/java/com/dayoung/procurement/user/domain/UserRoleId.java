package com.dayoung.procurement.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class UserRoleId implements Serializable {

	@Column(name = "app_user_id")
	private Long appUserId;

	@Column(name = "role_id")
	private Long roleId;

	protected UserRoleId() {
	}

	public UserRoleId(Long appUserId, Long roleId) {
		this.appUserId = appUserId;
		this.roleId = roleId;
	}

	public Long getAppUserId() {
		return appUserId;
	}

	public Long getRoleId() {
		return roleId;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof UserRoleId that)) {
			return false;
		}
		return Objects.equals(appUserId, that.appUserId) && Objects.equals(roleId, that.roleId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(appUserId, roleId);
	}
}
