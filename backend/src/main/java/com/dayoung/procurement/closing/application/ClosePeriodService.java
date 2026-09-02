package com.dayoung.procurement.closing.application;

import com.dayoung.procurement.closing.domain.ClosePeriodStatus;
import com.dayoung.procurement.closing.exception.ClosedPeriodException;
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

	@Transactional
	public void requireOpen(LocalDate postingDate) {
		String period = postingDate.format(PERIOD_FORMAT);
		closePeriodRepository.findByPeriodForUpdate(period)
				.filter(closePeriod -> closePeriod.getStatus() == ClosePeriodStatus.CLOSED)
				.ifPresent(closePeriod -> {
					throw new ClosedPeriodException(period);
				});
	}
}
