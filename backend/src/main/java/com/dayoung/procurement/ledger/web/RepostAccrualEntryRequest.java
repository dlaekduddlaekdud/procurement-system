package com.dayoung.procurement.ledger.web;

import com.dayoung.procurement.ledger.application.RepostAccrualEntryCommand;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.math.BigDecimal;
import java.time.LocalDate;

public record RepostAccrualEntryRequest(
		@NotNull @DecimalMin(value = "0.00", inclusive = false) @Digits(integer = 17, fraction = 2) BigDecimal amount,
		@NotNull @PastOrPresent LocalDate postingDate
) {
	RepostAccrualEntryCommand toCommand() {
		return new RepostAccrualEntryCommand(amount, postingDate);
	}
}
