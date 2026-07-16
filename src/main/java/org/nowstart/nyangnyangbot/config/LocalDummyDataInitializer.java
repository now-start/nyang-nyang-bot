package org.nowstart.nyangnyangbot.config;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.adapter.out.persistence.authorization.entity.AuthorizationAccount;
import org.nowstart.nyangnyangbot.adapter.out.persistence.authorization.repository.AuthorizationRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.favorite.entity.FavoriteAccount;
import org.nowstart.nyangnyangbot.adapter.out.persistence.favorite.entity.FavoriteAdjustment;
import org.nowstart.nyangnyangbot.adapter.out.persistence.favorite.entity.FavoriteHistory;
import org.nowstart.nyangnyangbot.adapter.out.persistence.favorite.repository.FavoriteAdjustmentRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.favorite.repository.FavoriteHistoryRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.favorite.repository.FavoriteRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.entity.RouletteEvent;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.entity.RouletteItem;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.entity.RouletteRoundResult;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.entity.RouletteTable;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.repository.RouletteEventRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.repository.RouletteItemRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.repository.RouletteRoundResultRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.repository.RouletteTableRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.timer.entity.TimerMessage;
import org.nowstart.nyangnyangbot.adapter.out.persistence.timer.repository.TimerMessageRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.upbo.entity.UpboTemplate;
import org.nowstart.nyangnyangbot.adapter.out.persistence.upbo.entity.UserUpbo;
import org.nowstart.nyangnyangbot.adapter.out.persistence.upbo.repository.UpboTemplateRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.upbo.repository.UserUpboRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.weekly.entity.WeeklyChatRank;
import org.nowstart.nyangnyangbot.adapter.out.persistence.weekly.repository.WeeklyChatRankRepository;
import org.nowstart.nyangnyangbot.domain.favorite.FavoriteSourceType;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
import org.nowstart.nyangnyangbot.domain.type.RouletteEventStatus;
import org.nowstart.nyangnyangbot.domain.type.RouletteRoundStatus;
import org.nowstart.nyangnyangbot.domain.type.UpboStatus;
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

    private static final List<FavoriteSeed> FAVORITE_SEEDS = List.of(
            new FavoriteSeed(LocalTestAccounts.ADMIN_USER_ID, "로컬 관리자", 9999),
            new FavoriteSeed(LocalTestAccounts.VIEWER_USER_ID, "일반 시청자", 875),
            new FavoriteSeed("user-001", "치즈냥", 8720),
            new FavoriteSeed("user-002", "새벽라떼", 7430),
            new FavoriteSeed("user-003", "민트초코", 6180),
            new FavoriteSeed("user-004", "고구마", 5050),
            new FavoriteSeed("user-005", "달토끼", 3920),
            new FavoriteSeed(LocalTestAccounts.NEGATIVE_USER_ID, "이불밖은위험해", -12),
            new FavoriteSeed("user-007", "파란별", 1410),
            new FavoriteSeed("user-008", "츄르도둑", 1240),
            new FavoriteSeed("user-009", "우주고양이", 986),
            new FavoriteSeed("user-010", "밤톨이", 642),
            new FavoriteSeed("user-011", "시금치파스타", 431),
            new FavoriteSeed("user-012", "감자탕수육", 333),
            new FavoriteSeed("user-013", "솜사탕", 310),
            new FavoriteSeed("user-014", "유자차", 286),
            new FavoriteSeed("user-015", "별사탕", 255),
            new FavoriteSeed("user-016", "모찌", 220),
            new FavoriteSeed("user-017", "초코칩", 197),
            new FavoriteSeed("user-018", "귤냥이", 166),
            new FavoriteSeed("user-019", "보리차", 144),
            new FavoriteSeed("user-020", "라임소다", 121),
            new FavoriteSeed("user-021", "홍차라떼", 108),
            new FavoriteSeed("user-022", "아침햇살", 96),
            new FavoriteSeed("user-023", "캣닢", 84),
            new FavoriteSeed("user-024", "바닐라", 72),
            new FavoriteSeed("user-025", "복숭아", 61),
            new FavoriteSeed("user-026", "구름빵", 48),
            new FavoriteSeed("user-027", "레몬밤", 37),
            new FavoriteSeed("user-028", "콩떡", 29),
            new FavoriteSeed("user-029", "밤하늘", 22),
            new FavoriteSeed("user-030", "민들레", 17),
            new FavoriteSeed("user-031", "새싹", 9),
            new FavoriteSeed("user-032", "먼지", 0)
    );

    private final AuthorizationRepository authorizationRepository;
    private final FavoriteRepository favoriteRepository;
    private final FavoriteHistoryRepository favoriteHistoryRepository;
    private final FavoriteAdjustmentRepository favoriteAdjustmentRepository;
    private final WeeklyChatRankRepository weeklyChatRankRepository;
    private final RouletteTableRepository rouletteTableRepository;
    private final RouletteItemRepository rouletteItemRepository;
    private final RouletteEventRepository rouletteEventRepository;
    private final RouletteRoundResultRepository rouletteRoundResultRepository;
    private final TimerMessageRepository timerMessageRepository;
    private final UpboTemplateRepository upboTemplateRepository;
    private final UserUpboRepository userUpboRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<FavoriteAccount> favorites = seedFavorites();
        seedLocalAuthorization();
        seedFavoriteHistories(favorites);
        seedWeeklyChatRanks();
        RouletteTable rouletteTable = seedRouletteTable();
        seedRouletteEvents(rouletteTable);
        seedTimerMessages();
        seedUpboTemplates();
        seedUserUpbos();
        seedFavoriteAdjustments();
        log.info("Local dummy data is ready");
    }

    private List<FavoriteAccount> seedFavorites() {
        List<FavoriteAccount> favorites = FAVORITE_SEEDS.stream()
                .map(seed -> favorite(seed.userId(), seed.nickName(), seed.favorite()))
                .toList();
        favorites.stream()
                .filter(favorite -> !favoriteRepository.existsById(favorite.getUserId()))
                .forEach(favoriteRepository::save);
        return favoriteRepository.findAllById(favorites.stream()
                .map(FavoriteAccount::getUserId)
                .toList());
    }

    private void seedLocalAuthorization() {
        LocalTestAccounts.accounts().stream()
                .filter(account -> !authorizationRepository.existsById(account.userId()))
                .map(account -> authorizationAccount(account.userId(), account.nickName(), account.admin()))
                .forEach(authorizationRepository::save);
    }

    private void seedFavoriteHistories(List<FavoriteAccount> favorites) {
        if (favoriteHistoryRepository.count() > 0) {
            return;
        }
        favoriteHistoryRepository.saveAll(favorites.stream()
                .flatMap(favorite -> favoriteHistories(favorite).stream())
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
                weeklyRank(weekStartDate, "user-005", "달토끼", 153),
                weeklyRank(weekStartDate, LocalTestAccounts.VIEWER_USER_ID, "일반 시청자", 128),
                weeklyRank(weekStartDate, "user-008", "츄르도둑", 114),
                weeklyRank(weekStartDate, "user-009", "우주고양이", 96),
                weeklyRank(weekStartDate, "user-010", "밤톨이", 83),
                weeklyRank(weekStartDate, "user-011", "시금치파스타", 75),
                weeklyRank(weekStartDate, LocalTestAccounts.NEGATIVE_USER_ID, "이불밖은위험해", 48),
                weeklyRank(weekStartDate, "user-012", "감자탕수육", 31)
        ));
    }

    private RouletteTable seedRouletteTable() {
        if (rouletteTableRepository.count() > 0) {
            return rouletteTableRepository.findAll().stream().findFirst().orElse(null);
        }
        RouletteTable table = rouletteTableRepository.save(RouletteTable.builder()
                .title("로컬 테스트 룰렛")
                .command("!룰렛")
                .pricePerRound(1000L)
                .active(true)
                .version(1)
                .highRoundThreshold(50)
                .build());
        rouletteItemRepository.saveAll(List.of(
                rouletteItem(table, "호감도 +500", 300, false, RewardType.FAVORITE, ConversionMode.AUTO, 500, 1),
                rouletteItem(table, "호감도 +300", 700, false, RewardType.FAVORITE, ConversionMode.AUTO, 300, 2),
                rouletteItem(table, "호감도 +100", 1800, false, RewardType.FAVORITE, ConversionMode.AUTO, 100, 3),
                rouletteItem(table, "호감도 +30", 2600, false, RewardType.FAVORITE, ConversionMode.AUTO, 30, 4),
                rouletteItem(table, "미션권", 900, false, RewardType.MISSION, ConversionMode.MANUAL, null, 5),
                rouletteItem(table, "쿠폰", 700, false, RewardType.COUPON, ConversionMode.MANUAL, null, 6),
                rouletteItem(table, "다음 기회에", 3000, true, RewardType.CUSTOM, ConversionMode.NONE, 0, 7)
        ));
        return table;
    }

    private void seedRouletteEvents(RouletteTable table) {
        if (table == null || rouletteEventRepository.count() > 0) {
            return;
        }
        RouletteEvent applied = rouletteEventRepository.save(rouletteEvent(
                table,
                "local-donation-001",
                "user-001",
                "치즈냥",
                11_000L,
                "!룰렛 11회",
                11,
                RouletteEventStatus.APPLIED
        ));
        rouletteRoundResultRepository.saveAll(IntStream.rangeClosed(1, 11)
                .mapToObj(roundNo -> rouletteRound(applied, roundNo, roundNo % 4 == 0 ? "다음 기회에" : "호감도 +100",
                        roundNo % 4 == 0, RouletteRoundStatus.APPLIED))
                .toList());

        RouletteEvent failed = rouletteEventRepository.save(rouletteEvent(
                table,
                "local-donation-002",
                "user-010",
                "밤톨이",
                3_000L,
                "!룰렛 테스트 실패 케이스",
                3,
                RouletteEventStatus.FAILED
        ));
        rouletteRoundResultRepository.saveAll(IntStream.rangeClosed(1, 3)
                .mapToObj(roundNo -> rouletteRound(failed, roundNo, "미션권", false, RouletteRoundStatus.FAILED))
                .toList());

        RouletteEvent partial = rouletteEventRepository.save(rouletteEvent(
                table,
                "local-donation-003",
                "user-012",
                "감자탕수육",
                5_000L,
                "!룰렛 부분 반영 케이스",
                5,
                RouletteEventStatus.PARTIALLY_APPLIED
        ));
        rouletteRoundResultRepository.saveAll(IntStream.rangeClosed(1, 5)
                .mapToObj(roundNo -> rouletteRound(partial, roundNo, roundNo == 5 ? "쿠폰" : "호감도 +30",
                        false, roundNo == 5 ? RouletteRoundStatus.CONFIRMED : RouletteRoundStatus.APPLIED))
                .toList());
    }

    private void seedTimerMessages() {
        if (timerMessageRepository.count() > 0) {
            return;
        }
        timerMessageRepository.save(TimerMessage.builder()
                .messageTemplate("잠시 후 방송이 계속됩니다. 현재 시각은 {time.time}입니다.")
                .intervalMinutes(30)
                .minChatCount(10)
                .active(false)
                .chatCountSinceLastSend(0)
                .claimedChatCount(0)
                .createdBy("local")
                .updatedBy("local")
                .build());
    }

    private void seedUpboTemplates() {
        if (upboTemplateRepository.count() > 0) {
            return;
        }
        upboTemplateRepository.saveAll(List.of(
                upboTemplate("호감도 +100", "즉시 호감도 100 지급", 100, RewardType.FAVORITE, ConversionMode.AUTO, 1),
                upboTemplate("호감도 +300", "고액 룰렛 보상 테스트", 300, RewardType.FAVORITE, ConversionMode.AUTO, 2),
                upboTemplate("미션 패스권", "미션 보상 테스트용 업보권", null, RewardType.MISSION, ConversionMode.MANUAL, 3),
                upboTemplate("참여 우선권", "이벤트 참여 우선권 테스트", null, RewardType.PARTICIPATION_PRIORITY, ConversionMode.MANUAL, 4),
                upboTemplate("쿠폰 수동 지급", "쿠폰 지급 플로우 확인용", null, RewardType.COUPON, ConversionMode.MANUAL, 5),
                upboTemplate("꽝 표시", "반영 없는 결과 표시 확인용", 0, RewardType.CUSTOM, ConversionMode.NONE, 6)
        ));
    }

    private void seedUserUpbos() {
        if (userUpboRepository.count() > 0) {
            return;
        }
        userUpboRepository.saveAll(List.of(
                userUpbo(LocalTestAccounts.VIEWER_USER_ID, "일반 시청자", "업보 차감권", UpboStatus.OWNED,
                        null, RewardType.COUPON, ConversionMode.MANUAL, FavoriteSourceType.UPBO_MANUAL, null,
                        "업보 차감권 보유", "local owned coupon"),
                userUpbo(LocalTestAccounts.VIEWER_USER_ID, "일반 시청자", "호감도 +100", UpboStatus.CONVERTED,
                        100, RewardType.FAVORITE, ConversionMode.AUTO, FavoriteSourceType.UPBO_ROULETTE, 101L,
                        "룰렛 보상 호감도 반영", "local converted reward"),
                userUpbo(LocalTestAccounts.VIEWER_USER_ID, "일반 시청자", "미션 패스권", UpboStatus.OWNED,
                        null, RewardType.MISSION, ConversionMode.MANUAL, FavoriteSourceType.UPBO_ROULETTE, null,
                        "방송 미션 패스권", "local mission reward"),
                userUpbo("user-001", "치즈냥", "참여 우선권", UpboStatus.USED,
                        null, RewardType.PARTICIPATION_PRIORITY, ConversionMode.MANUAL, FavoriteSourceType.UPBO_MANUAL, null,
                        "이벤트 참여 우선권 사용 완료", "local used reward"),
                userUpbo(LocalTestAccounts.NEGATIVE_USER_ID, "이불밖은위험해", "매너 채팅 업보", UpboStatus.OWNED,
                        -5, RewardType.CUSTOM, ConversionMode.MANUAL, FavoriteSourceType.UPBO_MANUAL, null,
                        "매너 채팅 주의 업보", "local negative upbo")
        ));
    }

    private void seedFavoriteAdjustments() {
        if (favoriteAdjustmentRepository.count() > 0) {
            return;
        }
        favoriteAdjustmentRepository.saveAll(List.of(
                favoriteAdjustment(100, "호감도 보너스"),
                favoriteAdjustment(10, "소소한 보너스"),
                favoriteAdjustment(-5, "업보 차감"),
                favoriteAdjustment(-50, "경고 차감"),
                favoriteAdjustment(1, "정정 +1")
        ));
    }

    private AuthorizationAccount authorizationAccount(String userId, String nickName, boolean admin) {
        return AuthorizationAccount.builder()
                .channelId(userId)
                .channelName(nickName)
                .accessToken("local-access-token-" + userId)
                .refreshToken("local-refresh-token-" + userId)
                .tokenType("Bearer")
                .expiresIn(3600)
                .scope("local")
                .favoriteHistoryLastSeenAt(LocalDateTime.now().minusDays(1))
                .admin(admin)
                .build();
    }

    private FavoriteAccount favorite(String userId, String nickName, Integer favorite) {
        return FavoriteAccount.builder()
                .userId(userId)
                .nickName(nickName)
                .favorite(favorite)
                .build();
    }

    private List<FavoriteHistory> favoriteHistories(FavoriteAccount favorite) {
        int current = favorite.getFavorite() == null ? 0 : favorite.getFavorite();
        int adminDelta = LocalTestAccounts.NEGATIVE_USER_ID.equals(favorite.getUserId()) ? -300 : 250;
        return List.of(
                history(favorite, FavoriteSourceType.ADMIN_ADJUSTMENT, adminDelta, current,
                        "운영자 조정", "local-admin-adjustment", "ADMIN"),
                history(favorite, FavoriteSourceType.ATTENDANCE, 100, current - adminDelta,
                        "출석 보너스", "local-attendance", "ATTENDANCE"),
                history(favorite, FavoriteSourceType.UPBO_ROULETTE, 30, current - adminDelta - 100,
                        "룰렛 보상", "local-roulette", "ROULETTE")
        );
    }

    private FavoriteHistory history(
            FavoriteAccount favorite,
            FavoriteSourceType sourceType,
            Integer delta,
            Integer balanceAfter,
            String description,
            String sourceId,
            String displayCategory
    ) {
        return FavoriteHistory.builder()
                .favoriteAccount(favorite)
                .history(description)
                .favorite(balanceAfter)
                .delta(delta)
                .balanceAfter(balanceAfter)
                .sourceType(sourceType)
                .sourceId(sourceId)
                .displayCategory(displayCategory)
                .publicDescription(description)
                .privateMemo("local dummy data")
                .actorId(LocalTestAccounts.ADMIN_USER_ID)
                .idempotencyKey("local-dummy-" + favorite.getUserId() + "-" + sourceId)
                .nickNameSnapshot(favorite.getNickName())
                .build();
    }

    private WeeklyChatRank weeklyRank(LocalDate weekStartDate, String userId, String nickName, long chatCount) {
        return WeeklyChatRank.builder()
                .weekStartDate(weekStartDate)
                .userId(userId)
                .nickName(nickName)
                .chatCount(chatCount)
                .build();
    }

    private RouletteItem rouletteItem(
            RouletteTable table,
            String label,
            Integer probabilityBasisPoints,
            boolean losingItem,
            RewardType rewardType,
            ConversionMode conversionMode,
            Integer exchangeFavoriteValue,
            Integer displayOrder
    ) {
        return RouletteItem.builder()
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

    private RouletteEvent rouletteEvent(
            RouletteTable table,
            String donationEventId,
            String userId,
            String nickName,
            Long donationAmount,
            String donationText,
            Integer roundCount,
            RouletteEventStatus status
    ) {
        return RouletteEvent.builder()
                .donationEventId(donationEventId)
                .idempotencyKey("local-" + donationEventId)
                .userId(userId)
                .nickNameSnapshot(nickName)
                .donationAmount(donationAmount)
                .donationText(donationText)
                .rouletteTableId(table.getId())
                .rouletteTableVersion(table.getVersion())
                .command(table.getCommand())
                .pricePerRound(table.getPricePerRound())
                .roundCount(roundCount)
                .itemsSnapshotJson("[]")
                .status(status)
                .build();
    }

    private RouletteRoundResult rouletteRound(
            RouletteEvent event,
            int roundNo,
            String itemLabel,
            boolean losingItem,
            RouletteRoundStatus status
    ) {
        return RouletteRoundResult.builder()
                .rouletteEvent(event)
                .roundNo(roundNo)
                .itemLabel(itemLabel)
                .probabilityBasisPoints(losingItem ? 3000 : 1800)
                .losingItem(losingItem)
                .rewardType(losingItem ? RewardType.CUSTOM : RewardType.FAVORITE)
                .conversionMode(losingItem ? ConversionMode.NONE : ConversionMode.AUTO)
                .exchangeFavoriteValue(losingItem ? 0 : 100)
                .status(status)
                .failureReason(status == RouletteRoundStatus.FAILED ? "로컬 실패 케이스" : null)
                .ticket(roundNo)
                .build();
    }

    private UpboTemplate upboTemplate(
            String label,
            String description,
            Integer exchangeFavoriteValue,
            RewardType rewardType,
            ConversionMode conversionMode,
            Integer displayOrder
    ) {
        return UpboTemplate.builder()
                .label(label)
                .description(description)
                .active(true)
                .displayOrder(displayOrder)
                .exchangeFavoriteValue(exchangeFavoriteValue)
                .rewardType(rewardType)
                .conversionMode(conversionMode)
                .build();
    }

    private UserUpbo userUpbo(
            String userId,
            String nickName,
            String label,
            UpboStatus status,
            Integer exchangeFavoriteValue,
            RewardType rewardType,
            ConversionMode conversionMode,
            FavoriteSourceType sourceType,
            Long ledgerId,
            String publicDescription,
            String privateMemo
    ) {
        return UserUpbo.builder()
                .userId(userId)
                .nickNameSnapshot(nickName)
                .label(label)
                .status(status)
                .exchangeFavoriteValue(exchangeFavoriteValue)
                .rewardType(rewardType)
                .conversionMode(conversionMode)
                .sourceType(sourceType)
                .ledgerId(ledgerId)
                .publicDescription(publicDescription)
                .privateMemo(privateMemo)
                .actorId(LocalTestAccounts.ADMIN_USER_ID)
                .build();
    }

    private FavoriteAdjustment favoriteAdjustment(Integer amount, String label) {
        return FavoriteAdjustment.builder()
                .amount(amount)
                .label(label)
                .build();
    }

    private record FavoriteSeed(String userId, String nickName, Integer favorite) {
    }
}
