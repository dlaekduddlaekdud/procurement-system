package com.dayoung.procurement.ledger.web;

import com.dayoung.procurement.ledger.application.ReverseAccrualEntryCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDate;

public record ReverseAccrualEntryRequest(
		@NotNull @PastOrPresent LocalDate postingDate
) {
	ReverseAccrualEntryCommand toCommand() {
		return new ReverseAccrualEntryCommand(postingDate);
	}
}
