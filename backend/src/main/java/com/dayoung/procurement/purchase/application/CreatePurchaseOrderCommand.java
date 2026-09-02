package com.dayoung.procurement.purchase.application;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreatePurchaseOrderCommand(
		@NotNull Long vendorId,
		@NotNull Long warehouseId,
		@FutureOrPresent LocalDate expectedDeliveryDate
) {
}
