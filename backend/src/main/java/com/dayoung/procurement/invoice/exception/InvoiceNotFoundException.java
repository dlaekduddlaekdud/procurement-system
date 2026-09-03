package com.dayoung.procurement.invoice.exception;

public class InvoiceNotFoundException extends RuntimeException {

	public InvoiceNotFoundException(Long invoiceId) {
		super("송장을 찾을 수 없습니다. invoiceId=%d".formatted(invoiceId));
	}
}
