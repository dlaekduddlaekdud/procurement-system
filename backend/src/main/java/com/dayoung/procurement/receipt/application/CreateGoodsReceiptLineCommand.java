package com.dayoung.procurement.receipt.application;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record CreateGoodsReceiptLineCommand(
		@NotNull Long purchaseOrderLineId,
		@NotNull @Positive @Digits(integer = 16, fraction = 3) BigDecimal quantity
) {
}
