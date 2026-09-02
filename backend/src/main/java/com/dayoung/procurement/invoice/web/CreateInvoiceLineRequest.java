package com.dayoung.procurement.invoice.web;

import com.dayoung.procurement.invoice.application.CreateInvoiceLineCommand;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record CreateInvoiceLineRequest(
		@NotNull Long purchaseOrderLineId,
		@NotNull @Positive @Digits(integer = 16, fraction = 3) BigDecimal quantity,
		@NotNull @PositiveOrZero @Digits(integer = 17, fraction = 2) BigDecimal unitPrice
) {
	CreateInvoiceLineCommand toCommand() {
		return new CreateInvoiceLineCommand(purchaseOrderLineId, quantity, unitPrice);
	}
}
