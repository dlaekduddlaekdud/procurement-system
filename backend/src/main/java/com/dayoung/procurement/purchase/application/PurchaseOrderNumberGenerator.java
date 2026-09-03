package com.dayoung.procurement.purchase.application;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PurchaseOrderNumberGenerator {

	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

	public String generate(LocalDate orderDate) {
		String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
		return "PO-%s-%s".formatted(orderDate.format(DATE_FORMAT), suffix);
	}
}
