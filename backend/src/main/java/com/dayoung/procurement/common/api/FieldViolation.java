package com.dayoung.procurement.common.api;

public record FieldViolation(
		String field,
		String message
) {
}
