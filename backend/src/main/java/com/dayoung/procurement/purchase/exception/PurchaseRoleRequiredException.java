package com.dayoung.procurement.purchase.exception;

import com.dayoung.procurement.user.domain.RoleCode;

public class PurchaseRoleRequiredException extends RuntimeException {

	public PurchaseRoleRequiredException(RoleCode roleCode) {
		super("%s 역할이 필요합니다.".formatted(roleCode));
	}
}
