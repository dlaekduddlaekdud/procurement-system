package com.dayoung.procurement.receipt.web;

import com.dayoung.procurement.receipt.application.CreateGoodsReceiptCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDate;
import java.util.List;

public record CreateGoodsReceiptRequest(
		@NotNull @PastOrPresent LocalDate postingDate,
		@NotEmpty List<@Valid CreateGoodsReceiptLineRequest> lines
) {
	CreateGoodsReceiptCommand toCommand() {
		return new CreateGoodsReceiptCommand(
				postingDate,
				lines.stream().map(CreateGoodsReceiptLineRequest::toCommand).toList()
		);
	}
}
