package com.dayoung.procurement.receipt.repository;

import com.dayoung.procurement.receipt.domain.GoodsReceipt;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GoodsReceiptRepository extends JpaRepository<GoodsReceipt, Long> {

	Optional<GoodsReceipt> findByReceiptNumber(String receiptNumber);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select receipt from GoodsReceipt receipt where receipt.id = :receiptId")
	Optional<GoodsReceipt> findByIdForAccrual(@Param("receiptId") Long receiptId);
}
