package com.dayoung.procurement.invoice.application;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record CreateInvoiceLineCommand(
		@NotNull Long purchaseOrderLineId,
		@NotNull @Positive @Digits(integer = 16, fraction = 3) BigDecimal quantity,
		@NotNull @PositiveOrZero @Digits(integer = 17, fraction = 2) BigDecimal unitPrice
) {
}
