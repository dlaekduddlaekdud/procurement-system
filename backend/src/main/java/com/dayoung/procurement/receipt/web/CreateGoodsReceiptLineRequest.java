package com.dayoung.procurement.receipt.web;

import com.dayoung.procurement.receipt.application.CreateGoodsReceiptLineCommand;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record CreateGoodsReceiptLineRequest(
		@NotNull Long purchaseOrderLineId,
		@NotNull @Positive @Digits(integer = 16, fraction = 3) BigDecimal quantity
) {
	CreateGoodsReceiptLineCommand toCommand() {
		return new CreateGoodsReceiptLineCommand(purchaseOrderLineId, quantity);
	}
}
