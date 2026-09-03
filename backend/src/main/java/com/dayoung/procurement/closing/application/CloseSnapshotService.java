package com.dayoung.procurement.closing.application;

import com.dayoung.procurement.closing.domain.CloseAccrualSnapshot;
import com.dayoung.procurement.closing.domain.CloseHoldSnapshot;
import com.dayoung.procurement.closing.domain.ClosePeriod;
import com.dayoung.procurement.closing.domain.CloseRun;
import com.dayoung.procurement.closing.repository.CloseAccrualSnapshotRepository;
import com.dayoung.procurement.closing.repository.CloseHoldSnapshotRepository;
import com.dayoung.procurement.ledger.repository.AccrualEntryRepository;
import com.dayoung.procurement.matching.domain.MatchingStatus;
import com.dayoung.procurement.matching.repository.MatchResultRepository;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CloseSnapshotService {

	private static final DateTimeFormatter PERIOD_FORMAT = DateTimeFormatter.ofPattern("yyyyMM");

	private final AccrualEntryRepository accrualEntryRepository;
	private final MatchResultRepository matchResultRepository;
	private final CloseAccrualSnapshotRepository accrualSnapshotRepository;
	private final CloseHoldSnapshotRepository holdSnapshotRepository;

	public CloseSnapshotService(
			AccrualEntryRepository accrualEntryRepository,
			MatchResultRepository matchResultRepository,
			CloseAccrualSnapshotRepository accrualSnapshotRepository,
			CloseHoldSnapshotRepository holdSnapshotRepository
	) {
		this.accrualEntryRepository = accrualEntryRepository;
		this.matchResultRepository = matchResultRepository;
		this.accrualSnapshotRepository = accrualSnapshotRepository;
		this.holdSnapshotRepository = holdSnapshotRepository;
	}

	public void createSnapshots(ClosePeriod closePeriod, CloseRun closeRun) {
		YearMonth targetMonth = YearMonth.parse(closePeriod.getPeriod(), PERIOD_FORMAT);
		List<CloseAccrualSnapshot> accrualSnapshots = accrualEntryRepository
				.findOutstandingBalancesThrough(targetMonth.atEndOfMonth())
				.stream()
				.map(balance -> new CloseAccrualSnapshot(
						closeRun,
						balance.getPurchaseOrderLine(),
						balance.getBalanceAmount(),
						balance.getCurrency()
				))
				.toList();
		List<CloseHoldSnapshot> holdSnapshots = matchResultRepository.findAllByStatusIn(List.of(
				MatchingStatus.HOLD_QUANTITY,
				MatchingStatus.HOLD_PRICE
		)).stream()
				.map(result -> new CloseHoldSnapshot(closeRun, result))
				.toList();

		accrualSnapshotRepository.saveAll(accrualSnapshots);
		holdSnapshotRepository.saveAll(holdSnapshots);
	}
}
