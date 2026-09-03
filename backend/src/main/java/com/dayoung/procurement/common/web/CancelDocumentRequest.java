package com.dayoung.procurement.common.web;

import com.dayoung.procurement.common.command.CancelDocumentCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDate;

public record CancelDocumentRequest(
		@NotNull @PastOrPresent LocalDate postingDate
) {
	public CancelDocumentCommand toCommand() {
		return new CancelDocumentCommand(postingDate);
	}
}
