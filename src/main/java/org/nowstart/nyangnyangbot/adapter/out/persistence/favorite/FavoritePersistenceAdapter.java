package org.nowstart.nyangnyangbot.adapter.out.persistence.favorite;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.domain.model.FavoriteHistoryView;
import org.nowstart.nyangnyangbot.domain.model.FavoriteSummary;
import org.nowstart.nyangnyangbot.application.port.out.favorite.repository.CheckIdempotencyPort;
import org.nowstart.nyangnyangbot.application.port.out.favorite.repository.FavoriteQueryPort;
import org.nowstart.nyangnyangbot.application.port.out.favorite.repository.LoadFavoriteAccountPort;
import org.nowstart.nyangnyangbot.application.port.out.favorite.repository.SaveFavoriteAccountPort;
import org.nowstart.nyangnyangbot.application.port.out.favorite.repository.SaveFavoriteLedgerPort;
import org.nowstart.nyangnyangbot.adapter.out.persistence.entity.FavoriteEntity;
import org.nowstart.nyangnyangbot.adapter.out.persistence.entity.FavoriteHistoryEntity;
import org.nowstart.nyangnyangbot.domain.favorite.FavoriteAccount;
import org.nowstart.nyangnyangbot.domain.favorite.FavoriteLedgerEntry;
import org.nowstart.nyangnyangbot.adapter.out.persistence.repository.FavoriteHistoryRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.repository.FavoriteRepository;
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
        return favoriteRepository.findByIdForUpdate(userId).map(this::toDomain);
    }

    @Override
    public FavoriteAccount save(FavoriteAccount account) {
        FavoriteEntity entity = favoriteRepository.findById(account.getUserId())
                .orElseGet(() -> FavoriteEntity.builder()
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
        FavoriteEntity favoriteEntity = favoriteRepository.getReferenceById(ledgerEntry.userId());
        FavoriteHistoryEntity saved = favoriteHistoryRepository.save(FavoriteHistoryEntity.builder()
                .favoriteEntity(favoriteEntity)
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
                .idempotencyKey(ledgerEntry.idempotencyKey())
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
                .idempotencyKey(ledgerEntry.idempotencyKey())
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
    public Page<FavoriteSummary> findAll(org.springframework.data.domain.Pageable pageable) {
        return favoriteRepository.findAll(pageable).map(this::toSummary);
    }

    @Override
    public Page<FavoriteSummary> findByNickNameContains(org.springframework.data.domain.Pageable pageable, String nickName) {
        return favoriteRepository.findByNickNameContains(pageable, nickName).map(this::toSummary);
    }

    @Override
    public Optional<FavoriteSummary> findById(String userId) {
        return favoriteRepository.findById(userId).map(this::toSummary);
    }

    @Override
    public FavoriteSummary getOrCreate(String userId, String nickName) {
        FavoriteEntity entity = favoriteRepository.findById(userId)
                .orElseGet(() -> favoriteRepository.save(FavoriteEntity.builder()
                        .userId(userId)
                        .nickName(nickName)
                        .favorite(0)
                        .build()));
        return toSummary(entity);
    }

    @Override
    public void updateNickName(String userId, String nickName) {
        favoriteRepository.findById(userId)
                .ifPresent(entity -> entity.setNickName(nickName));
    }

    @Override
    public List<FavoriteHistoryView> findHistory(String userId, int limit) {
        return favoriteHistoryRepository.findByFavoriteEntityUserId(
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
        return favoriteHistoryRepository.countByFavoriteEntityUserId(userId);
    }

    @Override
    public long countHistoryAfter(String userId, LocalDateTime createDate) {
        return favoriteHistoryRepository.countByFavoriteEntityUserIdAndCreateDateAfter(userId, createDate);
    }

    private FavoriteAccount toDomain(FavoriteEntity entity) {
        return FavoriteAccount.of(entity.getUserId(), entity.getNickName(), entity.getFavorite());
    }

    private FavoriteSummary toSummary(FavoriteEntity entity) {
        return new FavoriteSummary(entity.getUserId(), entity.getNickName(), entity.getFavorite());
    }

    private FavoriteHistoryView toHistoryView(FavoriteHistoryEntity entity) {
        Integer balanceAfter = entity.getBalanceAfter() == null ? entity.getFavorite() : entity.getBalanceAfter();
        String publicDescription = entity.getPublicDescription() == null ? entity.getHistory() : entity.getPublicDescription();
        String channelId = entity.getFavoriteEntity() == null ? null : entity.getFavoriteEntity().getUserId();
        return new FavoriteHistoryView(
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
}
