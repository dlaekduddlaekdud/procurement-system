package com.dayoung.procurement.purchase.exception;

public class SelfApprovalNotAllowedException extends RuntimeException {

	public SelfApprovalNotAllowedException() {
		super("자신이 작성한 구매요청은 승인하거나 거절할 수 없습니다.");
	}
}
