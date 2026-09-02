package com.dayoung.procurement.purchase.web;

import com.dayoung.procurement.purchase.application.UpdatePurchaseRequestCommand;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdatePurchaseRequestRequest(
		@NotBlank @Size(max = 200) String title,
		@Size(max = 1000) String purpose,
		@FutureOrPresent LocalDate neededDate
) {
	UpdatePurchaseRequestCommand toCommand() {
		return new UpdatePurchaseRequestCommand(title, purpose, neededDate);
	}
}
