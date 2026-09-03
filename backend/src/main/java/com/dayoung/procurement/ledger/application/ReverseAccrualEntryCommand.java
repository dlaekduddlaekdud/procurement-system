package com.dayoung.procurement.ledger.application;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDate;

public record ReverseAccrualEntryCommand(
		@NotNull @PastOrPresent LocalDate postingDate
) {
}
