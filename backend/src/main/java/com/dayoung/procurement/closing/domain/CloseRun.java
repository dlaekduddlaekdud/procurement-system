package com.dayoung.procurement.closing.domain;

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
import jakarta.persistence.Version;
import java.time.LocalDateTime;

@Entity
@Table(name = "close_run")
public class CloseRun {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "close_period_id", nullable = false)
	private ClosePeriod closePeriod;

	@Column(name = "attempt_no", nullable = false)
	private int attemptNo;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private CloseRunStatus status;

	@Enumerated(EnumType.STRING)
	@Column(name = "trigger_type", nullable = false, length = 20)
	private CloseRunTriggerType triggerType;

	@Column(name = "started_at", nullable = false)
	private LocalDateTime startedAt;

	@Column(name = "completed_at")
	private LocalDateTime completedAt;

	@Column(name = "failure_message", length = 1000)
	private String failureMessage;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "requested_by")
	private AppUser requestedBy;

	@Version
	@Column(nullable = false)
	private long version;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
	private LocalDateTime updatedAt;

	protected CloseRun() {
	}

	public CloseRun(
			ClosePeriod closePeriod,
			int attemptNo,
			CloseRunTriggerType triggerType,
			LocalDateTime startedAt,
			AppUser requestedBy
	) {
		if (closePeriod == null) {
			throw new IllegalArgumentException("마감 기간은 필수입니다.");
		}
		if (attemptNo <= 0) {
			throw new IllegalArgumentException("마감 시도 번호는 1 이상이어야 합니다.");
		}
		if (triggerType == null) {
			throw new IllegalArgumentException("마감 실행 방식은 필수입니다.");
		}
		if (startedAt == null) {
			throw new IllegalArgumentException("마감 시작 시각은 필수입니다.");
		}
		if (triggerType == CloseRunTriggerType.MANUAL && requestedBy == null) {
			throw new IllegalArgumentException("수동 마감 요청자는 필수입니다.");
		}
		if (triggerType == CloseRunTriggerType.SCHEDULED && requestedBy != null) {
			throw new IllegalArgumentException("자동 마감에는 요청자를 지정할 수 없습니다.");
		}
		this.closePeriod = closePeriod;
		this.attemptNo = attemptNo;
		this.status = CloseRunStatus.RUNNING;
		this.triggerType = triggerType;
		this.startedAt = startedAt;
		this.requestedBy = requestedBy;
	}

	public void succeed(LocalDateTime completedAt) {
		complete(CloseRunStatus.SUCCESS, completedAt, null);
	}

	public void fail(LocalDateTime completedAt, String failureMessage) {
		if (failureMessage == null || failureMessage.isBlank()) {
			throw new IllegalArgumentException("마감 실패 사유는 필수입니다.");
		}
		if (failureMessage.length() > 1000) {
			throw new IllegalArgumentException("마감 실패 사유는 1000자 이하여야 합니다.");
		}
		complete(CloseRunStatus.FAILED, completedAt, failureMessage);
	}

	public void skip(LocalDateTime completedAt) {
		complete(CloseRunStatus.SKIPPED, completedAt, null);
	}

	private void complete(CloseRunStatus completedStatus, LocalDateTime completedAt, String failureMessage) {
		if (status != CloseRunStatus.RUNNING) {
			throw new IllegalStateException("진행 중인 마감 실행만 완료할 수 있습니다.");
		}
		if (completedAt == null || completedAt.isBefore(startedAt)) {
			throw new IllegalArgumentException("마감 완료 시각은 시작 시각 이후여야 합니다.");
		}
		this.status = completedStatus;
		this.completedAt = completedAt;
		this.failureMessage = failureMessage;
	}

	public Long getId() {
		return id;
	}

	public ClosePeriod getClosePeriod() {
		return closePeriod;
	}

	public int getAttemptNo() {
		return attemptNo;
	}

	public CloseRunStatus getStatus() {
		return status;
	}

	public CloseRunTriggerType getTriggerType() {
		return triggerType;
	}

	public LocalDateTime getStartedAt() {
		return startedAt;
	}

	public LocalDateTime getCompletedAt() {
		return completedAt;
	}

	public String getFailureMessage() {
		return failureMessage;
	}

	public AppUser getRequestedBy() {
		return requestedBy;
	}

	public long getVersion() {
		return version;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}
}
