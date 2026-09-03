package com.dayoung.procurement.invoice.exception;

import com.dayoung.procurement.invoice.domain.InvoiceStatus;

public class InvalidInvoiceStateException extends RuntimeException {

	public InvalidInvoiceStateException(InvoiceStatus status) {
		super("송장 상태가 %s이므로 취소할 수 없습니다.".formatted(status));
	}
}
