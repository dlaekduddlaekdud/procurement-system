package com.dayoung.procurement.ledger.exception;

public class InvalidAccrualRepostingTargetException extends RuntimeException {

	public InvalidAccrualRepostingTargetException(Long entryId) {
		super("역분개 원장만 재기표할 수 있습니다. id=" + entryId);
	}
}
