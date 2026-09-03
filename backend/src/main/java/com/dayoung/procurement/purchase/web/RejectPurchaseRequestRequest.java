package com.dayoung.procurement.purchase.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectPurchaseRequestRequest(
		@NotBlank @Size(max = 500) String reason
) {
}
