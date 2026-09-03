package com.dayoung.procurement.ledger.exception;

public class AccrualEntryNotFoundException extends RuntimeException {

	public AccrualEntryNotFoundException(Long entryId) {
		super("원장 행을 찾을 수 없습니다. id=" + entryId);
	}
}
