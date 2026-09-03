package com.dayoung.procurement.closing.application;

import com.dayoung.procurement.closing.domain.ClosePeriod;
import com.dayoung.procurement.closing.repository.ClosePeriodRepository;
import org.springframework.stereotype.Component;

@Component
public class ClosePeriodLockService {

	private final ClosePeriodRepository closePeriodRepository;

	public ClosePeriodLockService(ClosePeriodRepository closePeriodRepository) {
		this.closePeriodRepository = closePeriodRepository;
	}

	public ClosePeriod getOrCreateForUpdate(String period) {
		// 기간 행이 없는 최초 동시 요청도 고유키와 행 잠금으로 직렬화한다.
		closePeriodRepository.createIfAbsent(period);
		return closePeriodRepository.findByPeriodForUpdate(period).orElseThrow();
	}
}
