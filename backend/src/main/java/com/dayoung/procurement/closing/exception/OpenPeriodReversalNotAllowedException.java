package com.dayoung.procurement.closing.exception;

public class OpenPeriodReversalNotAllowedException extends RuntimeException {

	public OpenPeriodReversalNotAllowedException(String period) {
		super("열린 기간의 원장은 마감 후 역분개 대상이 아닙니다. period=" + period);
	}
}
