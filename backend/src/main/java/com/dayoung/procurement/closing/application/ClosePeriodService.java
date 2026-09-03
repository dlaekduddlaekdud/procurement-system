package com.dayoung.procurement.closing.application;

import com.dayoung.procurement.closing.domain.ClosePeriodStatus;
import com.dayoung.procurement.closing.exception.ClosedPeriodException;
import com.dayoung.procurement.closing.exception.OpenPeriodReversalNotAllowedException;
import com.dayoung.procurement.closing.repository.ClosePeriodRepository;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ClosePeriodService {

	private static final DateTimeFormatter PERIOD_FORMAT = DateTimeFormatter.ofPattern("yyyyMM");

	private final ClosePeriodRepository closePeriodRepository;

	public ClosePeriodService(ClosePeriodRepository closePeriodRepository) {
		this.closePeriodRepository = closePeriodRepository;
	}

	/**
	 * 업무 문서의 posting_date가 속한 기간이 마감되지 않았는지 확인한다.
	 *
	 * <p>마감 여부 확인은 읽기 검증이므로 기간 행을 새로 만들지 않는다. 행이 없다는 것은 해당 기간을
	 * 한 번도 마감하지 않았다는 뜻이므로 열린 기간으로 판단한다. 진행 중인 마감과는 배제되어야 하므로
	 * 공유 락으로 읽고, 배타 락으로 올리지 않아 동시 등록끼리는 교착이 발생하지 않는다.
	 */
	@Transactional
	public void requireOpen(LocalDate postingDate) {
		String period = postingDate.format(PERIOD_FORMAT);
		closePeriodRepository.findByPeriodForShare(period)
				.filter(closePeriod -> closePeriod.getStatus() == ClosePeriodStatus.CLOSED)
				.ifPresent(closePeriod -> {
					throw new ClosedPeriodException(period);
				});
	}

	public void requireClosed(LocalDate postingDate) {
		String period = postingDate.format(PERIOD_FORMAT);
		boolean closed = closePeriodRepository.findByPeriodForShare(period)
				.filter(closePeriod -> closePeriod.getStatus() == ClosePeriodStatus.CLOSED)
				.isPresent();
		if (!closed) {
			throw new OpenPeriodReversalNotAllowedException(period);
		}
	}
}
