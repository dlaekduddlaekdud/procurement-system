package com.dayoung.procurement.audit.application;

import com.dayoung.procurement.audit.domain.AuditEventType;
import com.dayoung.procurement.audit.domain.AuditLog;
import com.dayoung.procurement.audit.domain.AuditTargetType;
import com.dayoung.procurement.audit.repository.AuditLogRepository;
import com.dayoung.procurement.user.domain.AppUser;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {

	private final AuditLogRepository auditLogRepository;

	public AuditLogService(AuditLogRepository auditLogRepository) {
		this.auditLogRepository = auditLogRepository;
	}

	public void record(
			AuditEventType eventType,
			AuditTargetType targetType,
			Long targetId,
			AppUser actor,
			String reason
	) {
		auditLogRepository.save(new AuditLog(eventType, targetType, targetId, actor, reason));
	}
}
