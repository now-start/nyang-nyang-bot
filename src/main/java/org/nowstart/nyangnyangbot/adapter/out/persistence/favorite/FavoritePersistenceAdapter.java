package org.nowstart.nyangnyangbot.adapter.out.persistence.favorite;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.application.port.out.favorite.FavoriteQueryPort.HistoryResult;
import org.nowstart.nyangnyangbot.application.port.out.favorite.FavoriteQueryPort.SummaryResult;
import org.nowstart.nyangnyangbot.application.port.out.favorite.CheckIdempotencyPort;
import org.nowstart.nyangnyangbot.application.port.out.favorite.FavoriteQueryPort;
import org.nowstart.nyangnyangbot.application.port.out.favorite.LoadFavoriteAccountPort;
import org.nowstart.nyangnyangbot.application.port.out.favorite.SaveFavoriteAccountPort;
import org.nowstart.nyangnyangbot.application.port.out.favorite.SaveFavoriteLedgerPort;
import org.nowstart.nyangnyangbot.adapter.out.persistence.favorite.entity.FavoriteHistory;
import org.nowstart.nyangnyangbot.adapter.out.persistence.favorite.repository.FavoriteHistoryRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.favorite.repository.FavoriteRepository;
import org.nowstart.nyangnyangbot.domain.favorite.FavoriteAccount;
import org.nowstart.nyangnyangbot.domain.favorite.FavoriteLedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FavoritePersistenceAdapter implements LoadFavoriteAccountPort, SaveFavoriteAccountPort,
        SaveFavoriteLedgerPort, CheckIdempotencyPort, FavoriteQueryPort {

    private final FavoriteRepository favoriteRepository;
    private final FavoriteHistoryRepository favoriteHistoryRepository;

    @Override
    public Optional<FavoriteAccount> loadForUpdate(String userId) {
        return favoriteRepository.findByIdForUpdate(userId).map(this::favoriteAccount);
    }

    @Override
    public FavoriteAccount save(FavoriteAccount account) {
        var entity = favoriteRepository.findById(account.getUserId())
                .orElseGet(() -> org.nowstart.nyangnyangbot.adapter.out.persistence.favorite.entity.FavoriteAccount.builder()
                        .userId(account.getUserId())
                        .favorite(0)
                        .build());
        entity.setNickName(account.getNickName());
        entity.setFavorite(account.getBalance());
        favoriteRepository.save(entity);
        return account;
    }

    @Override
    public FavoriteLedgerEntry save(FavoriteLedgerEntry ledgerEntry) {
        var favoriteAccount = favoriteRepository.getReferenceById(ledgerEntry.userId());
        String idempotencyKey = normalizeIdempotencyKey(ledgerEntry.idempotencyKey());
        FavoriteHistory saved = favoriteHistoryRepository.save(FavoriteHistory.builder()
                .favoriteAccount(favoriteAccount)
                .history(ledgerEntry.publicDescription())
                .favorite(ledgerEntry.balanceAfter())
                .delta(ledgerEntry.delta())
                .balanceAfter(ledgerEntry.balanceAfter())
                .sourceType(ledgerEntry.sourceType())
                .sourceId(ledgerEntry.sourceId())
                .displayCategory(ledgerEntry.displayCategory())
                .publicDescription(ledgerEntry.publicDescription())
                .privateMemo(ledgerEntry.privateMemo())
                .correctionOfLedgerId(ledgerEntry.correctionOfLedgerId())
                .actorId(ledgerEntry.actorId())
                .idempotencyKey(idempotencyKey)
                .nickNameSnapshot(ledgerEntry.nickNameSnapshot())
                .build());
        return FavoriteLedgerEntry.builder()
                .id(saved.getId())
                .userId(ledgerEntry.userId())
                .delta(ledgerEntry.delta())
                .balanceAfter(ledgerEntry.balanceAfter())
                .sourceType(ledgerEntry.sourceType())
                .sourceId(ledgerEntry.sourceId())
                .displayCategory(ledgerEntry.displayCategory())
                .publicDescription(ledgerEntry.publicDescription())
                .privateMemo(ledgerEntry.privateMemo())
                .correctionOfLedgerId(ledgerEntry.correctionOfLedgerId())
                .actorId(ledgerEntry.actorId())
                .idempotencyKey(idempotencyKey)
                .nickNameSnapshot(ledgerEntry.nickNameSnapshot())
                .build();
    }

    @Override
    public boolean existsByIdempotencyKey(String idempotencyKey) {
        return idempotencyKey != null
                && !idempotencyKey.isBlank()
                && favoriteHistoryRepository.existsByIdempotencyKey(idempotencyKey);
    }

    @Override
    public Page<SummaryResult> findAll(org.springframework.data.domain.Pageable pageable) {
        return favoriteRepository.findAll(pageable).map(this::toSummary);
    }

    @Override
    public Page<SummaryResult> findByNickNameContains(org.springframework.data.domain.Pageable pageable, String nickName) {
        return favoriteRepository.findByNickNameContains(pageable, nickName).map(this::toSummary);
    }

    @Override
    public Optional<SummaryResult> findById(String userId) {
        return favoriteRepository.findById(userId).map(this::toSummary);
    }

    @Override
    public SummaryResult getOrCreate(String userId, String nickName) {
        var entity = favoriteRepository.findById(userId)
                .orElseGet(() -> favoriteRepository.save(
                        org.nowstart.nyangnyangbot.adapter.out.persistence.favorite.entity.FavoriteAccount.builder()
                                .userId(userId)
                                .nickName(nickName)
                                .favorite(0)
                                .build()
                ));
        return toSummary(entity);
    }

    @Override
    public void updateNickName(String userId, String nickName) {
        favoriteRepository.findById(userId)
                .ifPresent(entity -> entity.setNickName(nickName));
    }

    @Override
    public List<HistoryResult> findHistory(String userId, int limit) {
        return favoriteHistoryRepository.findByFavoriteAccountUserId(
                        userId,
                        PageRequest.of(0, limit, Sort.by("createDate").descending())
                )
                .getContent()
                .stream()
                .map(this::toHistoryView)
                .toList();
    }

    @Override
    public long countHistory(String userId) {
        return favoriteHistoryRepository.countByFavoriteAccountUserId(userId);
    }

    @Override
    public long countHistoryAfter(String userId, LocalDateTime createDate) {
        return favoriteHistoryRepository.countByFavoriteAccountUserIdAndCreateDateAfter(userId, createDate);
    }

    private FavoriteAccount favoriteAccount(
            org.nowstart.nyangnyangbot.adapter.out.persistence.favorite.entity.FavoriteAccount entity
    ) {
        return FavoriteAccount.of(entity.getUserId(), entity.getNickName(), entity.getFavorite());
    }

    private SummaryResult toSummary(
            org.nowstart.nyangnyangbot.adapter.out.persistence.favorite.entity.FavoriteAccount entity
    ) {
        return new SummaryResult(entity.getUserId(), entity.getNickName(), entity.getFavorite());
    }

    private HistoryResult toHistoryView(FavoriteHistory entity) {
        Integer balanceAfter = entity.getBalanceAfter() == null ? entity.getFavorite() : entity.getBalanceAfter();
        String publicDescription = entity.getPublicDescription() == null ? entity.getHistory() : entity.getPublicDescription();
        String channelId = entity.getFavoriteAccount() == null ? null : entity.getFavoriteAccount().getUserId();
        return new HistoryResult(
                entity.getId(),
                channelId,
                entity.getNickNameSnapshot(),
                entity.getDelta(),
                balanceAfter,
                entity.getSourceType(),
                entity.getDisplayCategory(),
                publicDescription,
                entity.getCorrectionOfLedgerId() != null,
                entity.getFavorite(),
                entity.getHistory(),
                entity.getCreateDate()
        );
    }

    private String normalizeIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return null;
        }
        return idempotencyKey;
    }
}
