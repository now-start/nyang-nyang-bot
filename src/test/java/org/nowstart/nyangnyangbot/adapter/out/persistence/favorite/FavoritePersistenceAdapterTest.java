package org.nowstart.nyangnyangbot.adapter.out.persistence.favorite;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.adapter.out.persistence.favorite.entity.FavoriteAccount;
import org.nowstart.nyangnyangbot.adapter.out.persistence.favorite.entity.FavoriteHistory;
import org.nowstart.nyangnyangbot.adapter.out.persistence.favorite.repository.FavoriteHistoryRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.favorite.repository.FavoriteRepository;
import org.nowstart.nyangnyangbot.application.port.out.favorite.FavoriteQueryPort.HistoryResult;
import org.nowstart.nyangnyangbot.application.port.out.favorite.FavoriteQueryPort.SummaryResult;
import org.nowstart.nyangnyangbot.domain.favorite.FavoriteLedgerEntry;
import org.nowstart.nyangnyangbot.domain.favorite.FavoriteSourceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class FavoritePersistenceAdapterTest {

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private FavoriteHistoryRepository favoriteHistoryRepository;

    @Test
    void loadAndSaveAccount_ShouldMapFavoriteAccount() {
        // 준비
        FavoritePersistenceAdapter adapter = adapter();
        FavoriteAccount entity = favorite("user-1", "치즈냥", 10);
        given(favoriteRepository.findByIdForUpdate("user-1")).willReturn(Optional.of(entity));
        given(favoriteRepository.findById("user-1")).willReturn(Optional.of(entity));

        // 실행
        var loaded = adapter.loadForUpdate("user-1").orElseThrow();
        var saved = adapter.save(org.nowstart.nyangnyangbot.domain.favorite.FavoriteAccount.of("user-1", "치즈냥2", 20));

        // 검증
        then(loaded.getUserId()).isEqualTo("user-1");
        then(loaded.getBalance()).isEqualTo(10);
        then(saved.getBalance()).isEqualTo(20);
        then(entity.getNickName()).isEqualTo("치즈냥2");
        then(entity.getFavorite()).isEqualTo(20);
        BDDMockito.then(favoriteRepository).should().save(entity);
    }

    @Test
    void saveAccount_ShouldCreateEntityWhenMissing() {
        // 준비
        FavoritePersistenceAdapter adapter = adapter();
        given(favoriteRepository.findById("user-1")).willReturn(Optional.empty());

        // 실행
        var result = adapter.save(org.nowstart.nyangnyangbot.domain.favorite.FavoriteAccount.of("user-1", "치즈냥", 15));

        // 검증
        then(result.getBalance()).isEqualTo(15);
        BDDMockito.then(favoriteRepository).should().save(any(FavoriteAccount.class));
    }

    @Test
    void saveLedger_ShouldPersistHistoryAndReturnSavedId() {
        // 준비
        FavoritePersistenceAdapter adapter = adapter();
        FavoriteAccount favorite = favorite("user-1", "치즈냥", 20);
        FavoriteHistory savedHistory = history(99L, favorite, 5, 25, "공개 설명", "공개 설명");
        given(favoriteRepository.getReferenceById("user-1")).willReturn(favorite);
        given(favoriteHistoryRepository.save(any(FavoriteHistory.class))).willReturn(savedHistory);

        // 실행
        FavoriteLedgerEntry result = adapter.save(FavoriteLedgerEntry.builder()
                .userId("user-1")
                .delta(5)
                .balanceAfter(25)
                .sourceType(FavoriteSourceType.ADMIN_ADJUSTMENT)
                .sourceId("source-1")
                .displayCategory("ADMIN")
                .publicDescription("공개 설명")
                .privateMemo("내부 메모")
                .correctionOfLedgerId(1L)
                .actorId("admin-1")
                .idempotencyKey("key-1")
                .nickNameSnapshot("치즈냥")
                .build());

        // 검증
        then(result.id()).isEqualTo(99L);
        then(result.userId()).isEqualTo("user-1");
        then(result.delta()).isEqualTo(5);
        then(result.balanceAfter()).isEqualTo(25);
    }

    @Test
    void saveLedger_ShouldStoreBlankIdempotencyKeyAsNull() {
        // 준비
        FavoritePersistenceAdapter adapter = adapter();
        FavoriteAccount favorite = favorite("user-1", "치즈냥", 20);
        FavoriteHistory savedHistory = history(100L, favorite, 5, 25, "공개 설명", "공개 설명");
        ArgumentCaptor<FavoriteHistory> captor = ArgumentCaptor.forClass(FavoriteHistory.class);
        given(favoriteRepository.getReferenceById("user-1")).willReturn(favorite);
        given(favoriteHistoryRepository.save(any(FavoriteHistory.class))).willReturn(savedHistory);

        // 실행
        FavoriteLedgerEntry result = adapter.save(FavoriteLedgerEntry.builder()
                .userId("user-1")
                .delta(5)
                .balanceAfter(25)
                .sourceType(FavoriteSourceType.ADMIN_ADJUSTMENT)
                .sourceId("source-1")
                .displayCategory("ADMIN")
                .publicDescription("공개 설명")
                .idempotencyKey(" ")
                .nickNameSnapshot("치즈냥")
                .build());

        // 검증
        BDDMockito.then(favoriteHistoryRepository).should().save(captor.capture());
        then(captor.getValue().getIdempotencyKey()).isNull();
        then(result.idempotencyKey()).isNull();
    }

    @Test
    void idempotencyAndSummaryQueries_ShouldDelegateToRepositories() {
        // 준비
        FavoritePersistenceAdapter adapter = adapter();
        Pageable pageable = PageRequest.of(0, 10);
        FavoriteAccount favorite = favorite("user-1", "치즈냥", 30);
        given(favoriteHistoryRepository.existsByIdempotencyKey("key-1")).willReturn(true);
        given(favoriteRepository.findAll(pageable)).willReturn(new PageImpl<>(List.of(favorite)));
        given(favoriteRepository.findByNickNameContains(pageable, "치즈")).willReturn(new PageImpl<>(List.of(favorite)));
        given(favoriteRepository.findById("user-1")).willReturn(Optional.of(favorite));
        given(favoriteRepository.countByFavoriteGreaterThan(30)).willReturn(2L);

        // 실행
        boolean blankExists = adapter.existsByIdempotencyKey(" ");
        boolean exists = adapter.existsByIdempotencyKey("key-1");
        Page<SummaryResult> all = adapter.findAll(pageable);
        Page<SummaryResult> searched = adapter.findByNickNameContains(pageable, "치즈");
        Optional<SummaryResult> found = adapter.findById("user-1");
        long higherRankedUsers = adapter.countByFavoriteGreaterThan(30);

        // 검증
        then(blankExists).isFalse();
        then(exists).isTrue();
        then(all.getContent().getFirst().favorite()).isEqualTo(30);
        then(searched.getContent().getFirst().nickName()).isEqualTo("치즈냥");
        then(found).isPresent();
        then(higherRankedUsers).isEqualTo(2L);
    }

    @Test
    void getOrCreateAndUpdateNickname_ShouldUseExistingOrCreateNewEntity() {
        // 준비
        FavoritePersistenceAdapter adapter = adapter();
        FavoriteAccount existing = favorite("user-1", "치즈냥", 30);
        FavoriteAccount created = favorite("user-2", "새냥", 0);
        given(favoriteRepository.findById("user-1")).willReturn(Optional.of(existing));
        given(favoriteRepository.findById("user-2")).willReturn(Optional.empty());
        given(favoriteRepository.save(any(FavoriteAccount.class))).willReturn(created);
        given(favoriteRepository.findById("user-3")).willReturn(Optional.of(favorite("user-3", "이전", 5)));

        // 실행
        SummaryResult found = adapter.getOrCreate("user-1", "치즈냥");
        SummaryResult newOne = adapter.getOrCreate("user-2", "새냥");
        adapter.updateNickName("user-3", "변경");

        // 검증
        then(found.userId()).isEqualTo("user-1");
        then(newOne.userId()).isEqualTo("user-2");
        BDDMockito.then(favoriteRepository).should().save(any(FavoriteAccount.class));
    }

    @Test
    void historyQueries_ShouldMapFallbackFieldsAndCounts() {
        // 준비
        FavoritePersistenceAdapter adapter = adapter();
        FavoriteAccount favorite = favorite("user-1", "치즈냥", 30);
        LocalDateTime now = LocalDateTime.of(2026, 5, 16, 22, 30);
        FavoriteHistory modern = history(1L, favorite, 5, 35, "공개 설명", "이전 설명");
        FavoriteHistory legacy = history(2L, null, -3, null, null, "레거시 설명");
        given(favoriteHistoryRepository.findByFavoriteAccountUserId(any(), any()))
                .willReturn(new PageImpl<>(List.of(modern, legacy)));
        given(favoriteHistoryRepository.countByFavoriteAccountUserId("user-1")).willReturn(2L);
        given(favoriteHistoryRepository.countByFavoriteAccountUserIdAndCreateDateAfter("user-1", now)).willReturn(1L);

        // 실행
        List<HistoryResult> histories = adapter.findHistory("user-1", 10);
        long total = adapter.countHistory("user-1");
        long recent = adapter.countHistoryAfter("user-1", now);

        // 검증
        then(histories).hasSize(2);
        then(histories.get(0).channelId()).isEqualTo("user-1");
        then(histories.get(0).publicDescription()).isEqualTo("공개 설명");
        then(histories.get(1).channelId()).isNull();
        then(histories.get(1).balanceAfter()).isEqualTo(legacy.getFavorite());
        then(histories.get(1).publicDescription()).isEqualTo("레거시 설명");
        then(total).isEqualTo(2L);
        then(recent).isEqualTo(1L);
    }

    private FavoritePersistenceAdapter adapter() {
        return new FavoritePersistenceAdapter(favoriteRepository, favoriteHistoryRepository);
    }

    private FavoriteAccount favorite(String userId, String nickName, Integer favorite) {
        return FavoriteAccount.builder()
                .userId(userId)
                .nickName(nickName)
                .favorite(favorite)
                .build();
    }

    private FavoriteHistory history(
            Long id,
            FavoriteAccount favorite,
            Integer delta,
            Integer balanceAfter,
            String publicDescription,
            String history
    ) {
        return FavoriteHistory.builder()
                .id(id)
                .favoriteAccount(favorite)
                .history(history)
                .favorite(balanceAfter == null ? 10 : balanceAfter)
                .delta(delta)
                .balanceAfter(balanceAfter)
                .sourceType(FavoriteSourceType.ADMIN_ADJUSTMENT)
                .sourceId("source-1")
                .displayCategory("ADMIN")
                .publicDescription(publicDescription)
                .privateMemo("내부 메모")
                .correctionOfLedgerId(id == 1L ? 10L : null)
                .actorId("admin-1")
                .idempotencyKey("key-" + id)
                .nickNameSnapshot("치즈냥")
                .build();
    }
}
