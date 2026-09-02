package com.dayoung.procurement.receipt.application;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class GoodsReceiptNumberGenerator {

	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

	public String generate(LocalDate postingDate) {
		String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
		return "GR-%s-%s".formatted(postingDate.format(DATE_FORMAT), suffix);
	}
}
