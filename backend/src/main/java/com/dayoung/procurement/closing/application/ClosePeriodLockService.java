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

	/**
	 * 마감 실행이 기간 행을 배타 잠금한 상태에서 attempt를 부여하도록 한다.
	 *
	 * <p>기간 행이 아직 없는 최초 동시 요청은 잠글 대상이 없으므로 고유키와 {@code insert ignore}로
	 * 행을 먼저 확보한다. 등록과 잠금은 반드시 호출자와 같은 트랜잭션에서 수행해야 한다. 별도
	 * 트랜잭션으로 분리하면 호출자가 이미 잡고 있는 배타 잠금 때문에 등록이 스스로를 기다리다
	 * 잠금 대기 시간을 초과한다.
	 */
	public ClosePeriod getOrCreateForUpdate(String period) {
		closePeriodRepository.createIfAbsent(period);
		return closePeriodRepository.findByPeriodForUpdate(period).orElseThrow();
	}
}
