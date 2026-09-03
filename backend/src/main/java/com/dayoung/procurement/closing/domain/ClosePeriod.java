package com.dayoung.procurement.closing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;

@Entity
@Table(name = "close_period")
public class ClosePeriod {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 6, columnDefinition = "CHAR(6)")
	private String period;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ClosePeriodStatus status = ClosePeriodStatus.OPEN;

	@Column(name = "closed_at")
	private LocalDateTime closedAt;

	@Version
	@Column(nullable = false)
	private long version;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
	private LocalDateTime updatedAt;

	protected ClosePeriod() {
	}

	public ClosePeriod(String period) {
		if (period == null || !period.matches("\\d{6}")) {
			throw new IllegalArgumentException("마감 기간은 YYYYMM 형식이어야 합니다.");
		}
		this.period = period;
	}

	public void close(LocalDateTime closedAt) {
		if (closedAt == null) {
			throw new IllegalArgumentException("마감 시각은 필수입니다.");
		}
		this.status = ClosePeriodStatus.CLOSED;
		this.closedAt = closedAt;
	}

	public Long getId() {
		return id;
	}

	public String getPeriod() {
		return period;
	}

	public ClosePeriodStatus getStatus() {
		return status;
	}

	public LocalDateTime getClosedAt() {
		return closedAt;
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
