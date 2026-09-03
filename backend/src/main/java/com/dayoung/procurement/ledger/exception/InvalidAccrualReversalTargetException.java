package com.dayoung.procurement.ledger.exception;

public class InvalidAccrualReversalTargetException extends RuntimeException {

	public InvalidAccrualReversalTargetException(Long entryId) {
		super("원본 계상 원장만 역분개할 수 있습니다. id=" + entryId);
	}
}
