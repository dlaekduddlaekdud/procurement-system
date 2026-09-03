package com.dayoung.procurement.ledger.web;

import com.dayoung.procurement.ledger.application.ReverseAccrualEntryCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record ReverseAccrualEntryRequest(
		@NotNull @PastOrPresent LocalDate postingDate,
		@NotBlank @Size(max = 500) String reason
) {
	ReverseAccrualEntryCommand toCommand() {
		return new ReverseAccrualEntryCommand(postingDate, reason);
	}
}
