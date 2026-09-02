package com.dayoung.procurement.receipt.repository;

import com.dayoung.procurement.receipt.domain.GoodsReceipt;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoodsReceiptRepository extends JpaRepository<GoodsReceipt, Long> {

	Optional<GoodsReceipt> findByReceiptNumber(String receiptNumber);
}
