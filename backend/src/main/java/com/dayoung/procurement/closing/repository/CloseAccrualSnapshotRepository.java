package com.dayoung.procurement.closing.repository;

import com.dayoung.procurement.closing.domain.CloseAccrualSnapshot;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CloseAccrualSnapshotRepository extends JpaRepository<CloseAccrualSnapshot, Long> {

	List<CloseAccrualSnapshot> findAllByCloseRun_IdOrderByPurchaseOrderLine_Id(Long closeRunId);
}
