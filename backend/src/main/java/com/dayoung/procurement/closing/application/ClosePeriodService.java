package com.dayoung.procurement.closing.application;

import com.dayoung.procurement.closing.domain.ClosePeriodStatus;
import com.dayoung.procurement.closing.exception.ClosedPeriodException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ClosePeriodService {

	private static final DateTimeFormatter PERIOD_FORMAT = DateTimeFormatter.ofPattern("yyyyMM");

	private final ClosePeriodLockService closePeriodLockService;

	public ClosePeriodService(ClosePeriodLockService closePeriodLockService) {
		this.closePeriodLockService = closePeriodLockService;
	}

	@Transactional
	public void requireOpen(LocalDate postingDate) {
		String period = postingDate.format(PERIOD_FORMAT);
		if (closePeriodLockService.getOrCreateForUpdate(period).getStatus() == ClosePeriodStatus.CLOSED) {
			throw new ClosedPeriodException(period);
		}
	}
}
