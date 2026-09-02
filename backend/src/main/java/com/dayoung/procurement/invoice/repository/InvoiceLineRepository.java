package com.dayoung.procurement.invoice.repository;

import com.dayoung.procurement.invoice.domain.InvoiceLine;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceLineRepository extends JpaRepository<InvoiceLine, Long> {

	List<InvoiceLine> findAllByInvoice_IdOrderByLineNumber(Long invoiceId);

	List<InvoiceLine> findAllByPurchaseOrderLine_IdOrderById(Long purchaseOrderLineId);
}
