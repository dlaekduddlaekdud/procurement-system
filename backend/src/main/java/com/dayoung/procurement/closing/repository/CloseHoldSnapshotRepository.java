package com.dayoung.procurement.closing.repository;

import com.dayoung.procurement.closing.domain.CloseHoldSnapshot;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CloseHoldSnapshotRepository extends JpaRepository<CloseHoldSnapshot, Long> {

	List<CloseHoldSnapshot> findAllByCloseRun_IdOrderByPurchaseOrderLine_Id(Long closeRunId);
}
