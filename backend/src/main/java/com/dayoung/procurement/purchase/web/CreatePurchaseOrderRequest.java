package com.dayoung.procurement.purchase.web;

import com.dayoung.procurement.purchase.application.CreatePurchaseOrderCommand;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreatePurchaseOrderRequest(
		@NotNull Long vendorId,
		@NotNull Long warehouseId,
		@FutureOrPresent LocalDate expectedDeliveryDate
) {
	CreatePurchaseOrderCommand toCommand() {
		return new CreatePurchaseOrderCommand(vendorId, warehouseId, expectedDeliveryDate);
	}
}
