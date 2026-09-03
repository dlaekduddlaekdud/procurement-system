package com.dayoung.procurement.common.command;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDate;

public record CancelDocumentCommand(
		@NotNull @PastOrPresent LocalDate postingDate
) {
}
