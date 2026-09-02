package com.dayoung.procurement.invoice.exception;

public class DuplicateInvoiceException extends RuntimeException {

	public DuplicateInvoiceException(Long vendorId, String invoiceNumber) {
		super("같은 공급업체의 송장 번호가 이미 등록되어 있습니다. vendorId=%d, invoiceNumber=%s"
				.formatted(vendorId, invoiceNumber));
	}
}
