package org.nowstart.nyangnyangbot.application.service.point;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.application.port.in.point.QueryPointUseCase;
import org.nowstart.nyangnyangbot.application.port.out.point.PointQueryPort;
import org.nowstart.nyangnyangbot.application.port.out.point.PointQueryPort.PointHistoryRecord;
import org.nowstart.nyangnyangbot.application.port.out.point.PointQueryPort.PointSummaryRecord;
import org.nowstart.nyangnyangbot.application.port.out.user.UserAccountPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PointQueryService implements QueryPointUseCase {

    private final PointQueryPort pointQueryPort;
    private final UserAccountPort userAccountPort;

    @Override
    public Page<PointSummaryResult> getList(Pageable pageable) {
        return pointQueryPort.findAll(pageable).map(this::summary);
    }

    @Override
    public Page<PointSummaryResult> getByDisplayName(Pageable pageable, String displayName) {
        return pointQueryPort.findByDisplayName(pageable, displayName).map(this::summary);
    }

    @Override
    public List<PointHistoryResult> getHistory(String userId, int limit) {
        return pointQueryPort.findHistory(userId, limit).stream().map(this::history).toList();
    }

    @Override
    public PointMeResult getMyPoint(String userId) {
        PointSummaryRecord point = pointQueryPort.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User account not found"));
        return new PointMeResult(
                point.userId(),
                point.displayName(),
                point.balance(),
                pointQueryPort.countByBalanceGreaterThan(point.balance()) + 1,
                getHistory(userId, 50)
        );
    }

    @Override
    public Optional<String> getCurrentDisplayName(String userId) {
        return userAccountPort.findDisplayNameById(userId)
                .filter(name -> !name.isBlank());
    }

    private PointSummaryResult summary(PointSummaryRecord record) {
        return new PointSummaryResult(record.userId(), record.displayName(), record.balance());
    }

    private PointHistoryResult history(PointHistoryRecord record) {
        return new PointHistoryResult(
                record.ledgerId(),
                record.userId(),
                record.delta(),
                record.balanceAfter(),
                record.sourceType().name(),
                record.description(),
                record.correction(),
                record.createdAt()
        );
    }
}
