package com.dayoung.procurement.ledger.repository;

import com.dayoung.procurement.purchase.domain.PurchaseOrderLine;
import java.math.BigDecimal;

public interface AccrualBalanceView {

	PurchaseOrderLine getPurchaseOrderLine();

	String getCurrency();

	BigDecimal getBalanceAmount();
}
