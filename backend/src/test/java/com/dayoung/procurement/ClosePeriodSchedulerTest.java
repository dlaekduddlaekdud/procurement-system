package com.dayoung.procurement;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.dayoung.procurement.closing.application.ClosePeriodScheduler;
import com.dayoung.procurement.closing.application.CloseService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class ClosePeriodSchedulerTest {

	@Test
	void closesPreviousPeriodThroughCloseService() {
		CloseService closeService = mock(CloseService.class);
		Clock clock = Clock.fixed(
				Instant.parse("2026-09-30T15:05:00Z"),
				ZoneId.of("Asia/Seoul")
		);
		ClosePeriodScheduler scheduler = new ClosePeriodScheduler(closeService, clock);

		scheduler.closePreviousPeriod();

		verify(closeService).closeAutomatically("202609");
	}
}
