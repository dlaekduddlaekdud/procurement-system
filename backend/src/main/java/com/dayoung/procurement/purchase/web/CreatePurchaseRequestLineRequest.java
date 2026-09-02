package com.dayoung.procurement.purchase.web;

import com.dayoung.procurement.purchase.application.CreatePurchaseRequestLineCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreatePurchaseRequestLineRequest(
		@NotNull Long itemId,
		@NotNull @Positive BigDecimal quantity,
		@NotNull @PositiveOrZero BigDecimal estimatedUnitPrice,
		@Size(max = 500) String description
) {
	CreatePurchaseRequestLineCommand toCommand() {
		return new CreatePurchaseRequestLineCommand(itemId, quantity, estimatedUnitPrice, description);
	}
}
