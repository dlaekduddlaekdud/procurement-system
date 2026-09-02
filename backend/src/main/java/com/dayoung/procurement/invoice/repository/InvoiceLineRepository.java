package com.dayoung.procurement.invoice.repository;

import com.dayoung.procurement.invoice.domain.InvoiceLine;
import com.dayoung.procurement.invoice.domain.InvoiceStatus;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvoiceLineRepository extends JpaRepository<InvoiceLine, Long> {

	List<InvoiceLine> findAllByInvoice_IdOrderByLineNumber(Long invoiceId);

	List<InvoiceLine> findAllByPurchaseOrderLine_IdOrderById(Long purchaseOrderLineId);

	@Query("""
			select coalesce(sum(line.quantity), 0)
			from InvoiceLine line
			where line.purchaseOrderLine.id = :purchaseOrderLineId
			  and line.invoice.status = :status
			""")
	BigDecimal sumQuantityByPurchaseOrderLineIdAndStatus(
			@Param("purchaseOrderLineId") Long purchaseOrderLineId,
			@Param("status") InvoiceStatus status
	);

	@Query("""
			select coalesce(sum(line.supplyAmount), 0)
			from InvoiceLine line
			where line.purchaseOrderLine.id = :purchaseOrderLineId
			  and line.invoice.status = :status
			""")
	BigDecimal sumSupplyAmountByPurchaseOrderLineIdAndStatus(
			@Param("purchaseOrderLineId") Long purchaseOrderLineId,
			@Param("status") InvoiceStatus status
	);
}
