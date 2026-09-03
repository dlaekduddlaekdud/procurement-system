package com.dayoung.procurement.invoice.application;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record CreateInvoiceCommand(
		@NotBlank @Size(max = 50) String invoiceNumber,
		@NotNull @PastOrPresent LocalDate invoiceDate,
		@NotNull @PastOrPresent LocalDate postingDate,
		@NotEmpty List<@Valid CreateInvoiceLineCommand> lines
) {
}
