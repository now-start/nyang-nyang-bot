package org.nowstart.nyangnyangbot.adapter.out.persistence.point;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.adapter.out.persistence.point.entity.PointLedgerEntry;
import org.nowstart.nyangnyangbot.adapter.out.persistence.point.repository.PointLedgerEntryRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.point.repository.PointLedgerEntryRepository.PointHistoryProjection;
import org.nowstart.nyangnyangbot.adapter.out.persistence.point.repository.PointLedgerEntryRepository.PointSummaryProjection;
import org.nowstart.nyangnyangbot.adapter.out.persistence.user.entity.UserAccount;
import org.nowstart.nyangnyangbot.adapter.out.persistence.user.repository.UserAccountRepository;
import org.nowstart.nyangnyangbot.application.port.out.point.PointLedgerPort;
import org.nowstart.nyangnyangbot.application.port.out.point.PointQueryPort;
import org.nowstart.nyangnyangbot.domain.point.PointSourceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PointPersistenceAdapter implements PointLedgerPort, PointQueryPort {

    private final PointLedgerEntryRepository ledgerRepository;
    private final UserAccountRepository userAccountRepository;

    @Override
    public boolean lockUser(String userId, String displayName, boolean createIfMissing) {
        if (createIfMissing) {
            userAccountRepository.observe(userId, displayName);
        }
        Optional<UserAccount> account = userAccountRepository.findByIdForUpdate(userId);
        account.ifPresent(existing -> existing.observe(displayName));
        return account.isPresent();
    }

    @Override
    public long balance(String userId) {
        return ledgerRepository.balance(userId);
    }

    @Override
    public Optional<LedgerEntryRecord> findByIdempotencyKey(String idempotencyKey) {
        return ledgerRepository.findByIdempotencyKey(idempotencyKey).map(this::ledgerRecord);
    }

    @Override
    public Optional<LedgerEntryRecord> findCorrectionTargetForUpdate(long ledgerEntryId) {
        return ledgerRepository.findByIdForUpdate(ledgerEntryId).map(this::ledgerRecord);
    }

    @Override
    public boolean hasCorrection(long ledgerEntryId) {
        return ledgerRepository.existsByCorrectionOfEntryId(ledgerEntryId);
    }

    @Override
    public LedgerEntryRecord append(AppendPointEntry data) {
        UserAccount user = userAccountRepository.getReferenceById(data.userId());
        UserAccount actor = actor(data.actorUserId());
        PointLedgerEntry correction = data.correctionOfEntryId() == null
                ? null
                : ledgerRepository.getReferenceById(data.correctionOfEntryId());
        PointLedgerEntry saved = ledgerRepository.saveAndFlush(PointLedgerEntry.builder()
                .userAccount(user)
                .delta(data.delta())
                .sourceType(data.sourceType())
                .sourceReference(data.sourceReference())
                .description(data.description())
                .privateNote(data.privateNote())
                .correctionOfEntry(correction)
                .actorUser(actor)
                .idempotencyKey(data.idempotencyKey())
                .build());
        return ledgerRecord(saved);
    }

    @Override
    public Page<PointSummaryRecord> findAll(Pageable pageable) {
        return ledgerRepository.findPointSummaries(pageable).map(this::summaryRecord);
    }

    @Override
    public Page<PointSummaryRecord> findByDisplayName(Pageable pageable, String displayName) {
        return ledgerRepository.findPointSummariesByDisplayName(displayName, pageable).map(this::summaryRecord);
    }

    @Override
    public Optional<PointSummaryRecord> findByUserId(String userId) {
        return ledgerRepository.findPointSummary(userId).map(this::summaryRecord);
    }

    @Override
    public List<PointHistoryRecord> findHistory(String userId, int limit) {
        return ledgerRepository.findHistory(userId, limit).stream().map(this::historyRecord).toList();
    }

    @Override
    public Optional<Long> findBalanceByUserId(String userId) {
        if (!userAccountRepository.existsById(userId)) {
            return Optional.empty();
        }
        return Optional.of(ledgerRepository.balance(userId));
    }

    @Override
    public long countByBalanceGreaterThan(long balance) {
        return ledgerRepository.countUsersWithBalanceGreaterThan(balance);
    }

    private UserAccount actor(String actorUserId) {
        if (actorUserId == null || actorUserId.isBlank()) {
            return null;
        }
        return userAccountRepository.getReferenceById(actorUserId);
    }

    private LedgerEntryRecord ledgerRecord(PointLedgerEntry entry) {
        return new LedgerEntryRecord(
                entry.getId(),
                entry.getUserAccount().getUserId(),
                entry.getDelta(),
                entry.getSourceType(),
                entry.getSourceReference(),
                entry.getDescription(),
                entry.getPrivateNote(),
                entry.getCorrectionOfEntry() == null ? null : entry.getCorrectionOfEntry().getId(),
                entry.getActorUser() == null ? null : entry.getActorUser().getUserId()
        );
    }

    private PointSummaryRecord summaryRecord(PointSummaryProjection projection) {
        return new PointSummaryRecord(
                projection.getUserId(),
                projection.getDisplayName(),
                projection.getBalance() == null ? 0L : projection.getBalance()
        );
    }

    private PointHistoryRecord historyRecord(PointHistoryProjection projection) {
        return new PointHistoryRecord(
                projection.getLedgerId(),
                projection.getUserId(),
                projection.getDelta(),
                projection.getBalanceAfter(),
                PointSourceType.valueOf(projection.getSourceType()),
                projection.getDescription(),
                projection.getCorrectionOfEntryId() != null,
                projection.getCreatedAt()
        );
    }
}
