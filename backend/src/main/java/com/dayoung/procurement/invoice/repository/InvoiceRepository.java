package com.dayoung.procurement.invoice.repository;

import com.dayoung.procurement.invoice.domain.Invoice;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

	Optional<Invoice> findByVendor_IdAndInvoiceNumber(Long vendorId, String invoiceNumber);

	boolean existsByVendor_IdAndInvoiceNumber(Long vendorId, String invoiceNumber);
}
