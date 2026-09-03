package com.dayoung.procurement.closing.application;

import java.time.Clock;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ClosePeriodScheduler {

	private static final DateTimeFormatter PERIOD_FORMAT = DateTimeFormatter.ofPattern("yyyyMM");

	private final CloseService closeService;
	private final Clock clock;

	public ClosePeriodScheduler(CloseService closeService, Clock clock) {
		this.closeService = closeService;
		this.clock = clock;
	}

	@Scheduled(
			cron = "${procurement.closing.schedule.cron:0 5 0 1 * *}",
			zone = "${procurement.closing.schedule.zone:Asia/Seoul}"
	)
	public void closePreviousPeriod() {
		String previousPeriod = YearMonth.now(clock)
				.minusMonths(1)
				.format(PERIOD_FORMAT);
		closeService.closeAutomatically(previousPeriod);
	}
}
