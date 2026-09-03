package com.dayoung.procurement.invoice.repository;

import com.dayoung.procurement.invoice.domain.Invoice;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

	Optional<Invoice> findByVendor_IdAndInvoiceNumber(Long vendorId, String invoiceNumber);

	boolean existsByVendor_IdAndInvoiceNumber(Long vendorId, String invoiceNumber);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select invoice from Invoice invoice where invoice.id = :invoiceId")
	Optional<Invoice> findByIdForUpdate(@Param("invoiceId") Long invoiceId);
}
