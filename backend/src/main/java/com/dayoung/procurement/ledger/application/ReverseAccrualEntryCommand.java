package com.dayoung.procurement.ledger.application;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record ReverseAccrualEntryCommand(
		@NotNull @PastOrPresent LocalDate postingDate,
		@NotBlank @Size(max = 500) String reason
) {
}
