package com.dayoung.procurement.closing.exception;

public class ClosedPeriodException extends RuntimeException {

	public ClosedPeriodException(String period) {
		super("마감된 기간에는 입고를 등록할 수 없습니다. period=%s".formatted(period));
	}
}
