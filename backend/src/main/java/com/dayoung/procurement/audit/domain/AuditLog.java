package com.dayoung.procurement.audit.domain;

import com.dayoung.procurement.user.domain.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log")
public class AuditLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "event_type", nullable = false, length = 40)
	private AuditEventType eventType;

	@Enumerated(EnumType.STRING)
	@Column(name = "target_type", nullable = false, length = 30)
	private AuditTargetType targetType;

	@Column(name = "target_id", nullable = false)
	private Long targetId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "actor_id", nullable = false)
	private AppUser actor;

	@Column(nullable = false, length = 500)
	private String reason;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private LocalDateTime createdAt;

	protected AuditLog() {
	}

	public AuditLog(
			AuditEventType eventType,
			AuditTargetType targetType,
			Long targetId,
			AppUser actor,
			String reason
	) {
		this.eventType = eventType;
		this.targetType = targetType;
		this.targetId = targetId;
		this.actor = actor;
		this.reason = reason;
	}

	public Long getId() {
		return id;
	}

	public AuditEventType getEventType() {
		return eventType;
	}

	public AuditTargetType getTargetType() {
		return targetType;
	}

	public Long getTargetId() {
		return targetId;
	}

	public AppUser getActor() {
		return actor;
	}

	public String getReason() {
		return reason;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}
