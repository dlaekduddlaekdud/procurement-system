package com.dayoung.procurement.ledger.exception;

public class DuplicateAccrualRepostingException extends RuntimeException {

	public DuplicateAccrualRepostingException(Long reversalId) {
		super("이미 재기표된 역분개 원장입니다. reversalId=" + reversalId);
	}
}
