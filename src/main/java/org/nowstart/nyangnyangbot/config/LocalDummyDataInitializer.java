package org.nowstart.nyangnyangbot.config;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.adapter.out.persistence.authorization.entity.AuthorizationEntity;
import org.nowstart.nyangnyangbot.adapter.out.persistence.favorite.entity.FavoriteEntity;
import org.nowstart.nyangnyangbot.adapter.out.persistence.favorite.entity.FavoriteHistoryEntity;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.entity.RouletteItemEntity;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.entity.RouletteTableEntity;
import org.nowstart.nyangnyangbot.adapter.out.persistence.upbo.entity.UpboTemplateEntity;
import org.nowstart.nyangnyangbot.adapter.out.persistence.weekly.entity.WeeklyChatRankEntity;
import org.nowstart.nyangnyangbot.adapter.out.persistence.authorization.repository.AuthorizationRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.favorite.repository.FavoriteHistoryRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.favorite.repository.FavoriteRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.repository.RouletteItemRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.repository.RouletteTableRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.upbo.repository.UpboTemplateRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.weekly.repository.WeeklyChatRankRepository;
import org.nowstart.nyangnyangbot.domain.favorite.FavoriteSourceType;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "nyang.local-dummy-data", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LocalDummyDataInitializer implements ApplicationRunner {

    private static final String LOCAL_CHANNEL_ID = "local-channel";

    private final AuthorizationRepository authorizationRepository;
    private final FavoriteRepository favoriteRepository;
    private final FavoriteHistoryRepository favoriteHistoryRepository;
    private final WeeklyChatRankRepository weeklyChatRankRepository;
    private final RouletteTableRepository rouletteTableRepository;
    private final RouletteItemRepository rouletteItemRepository;
    private final UpboTemplateRepository upboTemplateRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<FavoriteEntity> favorites = seedFavorites();
        seedLocalAuthorization();
        seedFavoriteHistories(favorites);
        seedWeeklyChatRanks();
        seedRouletteTable();
        seedUpboTemplates();
        log.info("Local dummy data is ready");
    }

    private List<FavoriteEntity> seedFavorites() {
        List<FavoriteEntity> favorites = List.of(
                favorite(LOCAL_CHANNEL_ID, "로컬 관리자", 9999),
                favorite("user-001", "치즈냥", 8720),
                favorite("user-002", "새벽라떼", 7430),
                favorite("user-003", "민트초코", 6180),
                favorite("user-004", "고구마", 5050),
                favorite("user-005", "달토끼", 3920),
                favorite("user-006", "감자전", 2770),
                favorite("user-007", "파란별", 1410)
        );
        favorites.stream()
                .filter(favorite -> !favoriteRepository.existsById(favorite.getUserId()))
                .forEach(favoriteRepository::save);
        return favoriteRepository.findAllById(favorites.stream()
                .map(FavoriteEntity::getUserId)
                .toList());
    }

    private void seedLocalAuthorization() {
        if (authorizationRepository.existsById(LOCAL_CHANNEL_ID)) {
            return;
        }
        authorizationRepository.save(AuthorizationEntity.builder()
                .channelId(LOCAL_CHANNEL_ID)
                .channelName("로컬 관리자")
                .accessToken("local-access-token")
                .refreshToken("local-refresh-token")
                .tokenType("Bearer")
                .expiresIn(3600)
                .scope("local")
                .favoriteHistoryLastSeenAt(LocalDateTime.now().minusDays(1))
                .admin(true)
                .build());
    }

    private void seedFavoriteHistories(List<FavoriteEntity> favorites) {
        if (favoriteHistoryRepository.count() > 0) {
            return;
        }
        favoriteHistoryRepository.saveAll(favorites.stream()
                .flatMap(favorite -> List.of(
                        history(favorite, FavoriteSourceType.ATTENDANCE, 100, "출석 보너스", "local-attendance"),
                        history(favorite, FavoriteSourceType.ADMIN_ADJUSTMENT, 250, "운영자 지급", "local-admin-adjustment")
                ).stream())
                .toList());
    }

    private void seedWeeklyChatRanks() {
        if (weeklyChatRankRepository.count() > 0) {
            return;
        }
        LocalDate weekStartDate = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        weeklyChatRankRepository.saveAll(List.of(
                weeklyRank(weekStartDate, "user-001", "치즈냥", 421),
                weeklyRank(weekStartDate, "user-002", "새벽라떼", 387),
                weeklyRank(weekStartDate, "user-003", "민트초코", 244),
                weeklyRank(weekStartDate, "user-004", "고구마", 199),
                weeklyRank(weekStartDate, "user-005", "달토끼", 153)
        ));
    }

    private void seedRouletteTable() {
        if (rouletteTableRepository.count() > 0) {
            return;
        }
        RouletteTableEntity table = rouletteTableRepository.save(RouletteTableEntity.builder()
                .title("로컬 테스트 룰렛")
                .command("!룰렛")
                .pricePerRound(1000L)
                .active(true)
                .version(1)
                .highRoundThreshold(50)
                .build());
        rouletteItemRepository.saveAll(List.of(
                rouletteItem(table, "호감도 +300", 500, false, RewardType.FAVORITE, ConversionMode.AUTO, 300, 1),
                rouletteItem(table, "호감도 +100", 1500, false, RewardType.FAVORITE, ConversionMode.AUTO, 100, 2),
                rouletteItem(table, "호감도 +30", 2500, false, RewardType.FAVORITE, ConversionMode.AUTO, 30, 3),
                rouletteItem(table, "미션권", 1000, false, RewardType.MISSION, ConversionMode.MANUAL, null, 4),
                rouletteItem(table, "다음 기회에", 4500, true, RewardType.CUSTOM, ConversionMode.NONE, 0, 5)
        ));
    }

    private void seedUpboTemplates() {
        if (upboTemplateRepository.count() > 0) {
            return;
        }
        upboTemplateRepository.saveAll(List.of(
                upboTemplate("호감도 +100", "즉시 호감도 100 지급", 100, RewardType.FAVORITE, ConversionMode.AUTO, 1),
                upboTemplate("미션 패스권", "미션 보상 테스트용 업보권", null, RewardType.MISSION, ConversionMode.MANUAL, 2),
                upboTemplate("참여 우선권", "이벤트 참여 우선권 테스트", null, RewardType.PARTICIPATION_PRIORITY, ConversionMode.MANUAL, 3)
        ));
    }

    private FavoriteEntity favorite(String userId, String nickName, Integer favorite) {
        return FavoriteEntity.builder()
                .userId(userId)
                .nickName(nickName)
                .favorite(favorite)
                .build();
    }

    private FavoriteHistoryEntity history(
            FavoriteEntity favorite,
            FavoriteSourceType sourceType,
            Integer delta,
            String description,
            String sourceId
    ) {
        int balanceAfter = favorite.getFavorite();
        return FavoriteHistoryEntity.builder()
                .favoriteEntity(favorite)
                .history(description)
                .favorite(balanceAfter)
                .delta(delta)
                .balanceAfter(balanceAfter)
                .sourceType(sourceType)
                .sourceId(sourceId)
                .displayCategory("로컬 테스트")
                .publicDescription(description)
                .privateMemo("local dummy data")
                .actorId(LOCAL_CHANNEL_ID)
                .idempotencyKey("local-dummy-" + favorite.getUserId() + "-" + sourceId)
                .nickNameSnapshot(favorite.getNickName())
                .build();
    }

    private WeeklyChatRankEntity weeklyRank(LocalDate weekStartDate, String userId, String nickName, long chatCount) {
        return WeeklyChatRankEntity.builder()
                .weekStartDate(weekStartDate)
                .userId(userId)
                .nickName(nickName)
                .chatCount(chatCount)
                .build();
    }

    private RouletteItemEntity rouletteItem(
            RouletteTableEntity table,
            String label,
            Integer probabilityBasisPoints,
            boolean losingItem,
            RewardType rewardType,
            ConversionMode conversionMode,
            Integer exchangeFavoriteValue,
            Integer displayOrder
    ) {
        return RouletteItemEntity.builder()
                .rouletteTable(table)
                .label(label)
                .probabilityBasisPoints(probabilityBasisPoints)
                .losingItem(losingItem)
                .rewardType(rewardType)
                .conversionMode(conversionMode)
                .exchangeFavoriteValue(exchangeFavoriteValue)
                .active(true)
                .displayOrder(displayOrder)
                .build();
    }

    private UpboTemplateEntity upboTemplate(
            String label,
            String description,
            Integer exchangeFavoriteValue,
            RewardType rewardType,
            ConversionMode conversionMode,
            Integer displayOrder
    ) {
        return UpboTemplateEntity.builder()
                .label(label)
                .description(description)
                .active(true)
                .displayOrder(displayOrder)
                .exchangeFavoriteValue(exchangeFavoriteValue)
                .rewardType(rewardType)
                .conversionMode(conversionMode)
                .build();
    }
}
