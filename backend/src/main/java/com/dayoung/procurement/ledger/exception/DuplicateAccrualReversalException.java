package com.dayoung.procurement.ledger.exception;

public class DuplicateAccrualReversalException extends RuntimeException {

	public DuplicateAccrualReversalException(Long entryId) {
		super("이미 역분개된 원장 행입니다. id=" + entryId);
	}
}
