package com.dayoung.procurement.purchase.application;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record CreatePurchaseRequestCommand(
		@NotBlank @Size(max = 200) String title,
		@Size(max = 1000) String purpose,
		@FutureOrPresent LocalDate neededDate,
		@NotEmpty List<@Valid CreatePurchaseRequestLineCommand> lines
) {
}
