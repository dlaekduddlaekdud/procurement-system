package com.dayoung.procurement.receipt.application;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDate;
import java.util.List;

public record CreateGoodsReceiptCommand(
		@NotNull @PastOrPresent LocalDate postingDate,
		@NotEmpty List<@Valid CreateGoodsReceiptLineCommand> lines
) {
}
