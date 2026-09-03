package com.dayoung.procurement.common.web;

import com.dayoung.procurement.common.command.CancelDocumentCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CancelDocumentRequest(
		@NotNull @PastOrPresent LocalDate postingDate,
		@NotBlank @Size(max = 500) String reason
) {
	public CancelDocumentCommand toCommand() {
		return new CancelDocumentCommand(postingDate, reason);
	}
}
