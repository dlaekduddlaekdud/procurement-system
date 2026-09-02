package com.dayoung.procurement.purchase.application;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PurchaseRequestNumberGenerator {

	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

	public String generate(LocalDate requestDate) {
		String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
		return "PR-%s-%s".formatted(requestDate.format(DATE_FORMAT), suffix);
	}
}
