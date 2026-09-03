package com.dayoung.procurement.audit.repository;

import com.dayoung.procurement.audit.domain.AuditLog;
import com.dayoung.procurement.audit.domain.AuditTargetType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

	List<AuditLog> findAllByTargetTypeAndTargetIdOrderById(AuditTargetType targetType, Long targetId);
}
