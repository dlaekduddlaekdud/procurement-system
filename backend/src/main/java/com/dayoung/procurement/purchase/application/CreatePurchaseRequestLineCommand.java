package com.dayoung.procurement.purchase.application;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreatePurchaseRequestLineCommand(
		@NotNull Long itemId,
		@NotNull @Positive BigDecimal quantity,
		@NotNull @PositiveOrZero BigDecimal estimatedUnitPrice,
		@Size(max = 500) String description
) {
}
