package com.dayoung.procurement.closing.application;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class ClosingSchedulerConfiguration {

	@Bean
	Clock closingClock(@Value("${procurement.closing.schedule.zone:Asia/Seoul}") String zone) {
		return Clock.system(ZoneId.of(zone));
	}
}
