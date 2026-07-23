package org.nowstart.nyangnyangbot.config;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.adapter.out.persistence.command.entity.Command;
import org.nowstart.nyangnyangbot.adapter.out.persistence.command.repository.CommandRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.point.entity.PointAdjustmentPreset;
import org.nowstart.nyangnyangbot.adapter.out.persistence.point.entity.PointLedgerEntry;
import org.nowstart.nyangnyangbot.adapter.out.persistence.point.repository.PointAdjustmentPresetRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.point.repository.PointLedgerEntryRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.entity.RouletteConfig;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.entity.RouletteOption;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.repository.RouletteConfigRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.repository.RouletteOptionRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.timer.entity.TimerMessage;
import org.nowstart.nyangnyangbot.adapter.out.persistence.timer.repository.TimerMessageRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.user.entity.OAuthCredential;
import org.nowstart.nyangnyangbot.adapter.out.persistence.user.entity.UserAccount;
import org.nowstart.nyangnyangbot.adapter.out.persistence.user.repository.OAuthCredentialRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.user.repository.UserAccountRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.weekly.entity.WeeklyChatCount;
import org.nowstart.nyangnyangbot.adapter.out.persistence.weekly.repository.WeeklyChatCountRepository;
import org.nowstart.nyangnyangbot.domain.command.CommandExecutionPolicy;
import org.nowstart.nyangnyangbot.domain.point.PointSourceType;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
import org.nowstart.nyangnyangbot.domain.type.RouletteConfigStatus;
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

    private static final List<PointSeed> POINT_SEEDS = List.of(
            new PointSeed(LocalTestAccounts.ADMIN_USER_ID, "로컬 관리자", true, 9_999),
            new PointSeed(LocalTestAccounts.VIEWER_USER_ID, "일반 시청자", false, 875),
            new PointSeed("user-001", "치즈냥", false, 8_720),
            new PointSeed("user-002", "새벽라떼", false, 7_430),
            new PointSeed("user-003", "민트초코", false, 6_180),
            new PointSeed("user-004", "고구마", false, 5_050),
            new PointSeed("user-005", "달토끼", false, 3_920),
            new PointSeed(LocalTestAccounts.NEGATIVE_USER_ID, "이불밖은위험해", false, -12),
            new PointSeed("user-007", "파란별", false, 1_410),
            new PointSeed("user-008", "츄르도둑", false, 1_240),
            new PointSeed("user-009", "우주고양이", false, 986),
            new PointSeed("user-010", "밤톨이", false, 642),
            new PointSeed("user-011", "시금치파스타", false, 431),
            new PointSeed("user-012", "감자탕수육", false, 333),
            new PointSeed("user-013", "솜사탕", false, 310),
            new PointSeed("user-014", "유자차", false, 286),
            new PointSeed("user-015", "별사탕", false, 255),
            new PointSeed("user-016", "모찌", false, 220),
            new PointSeed("user-017", "초코칩", false, 197),
            new PointSeed("user-018", "귤냥이", false, 166),
            new PointSeed("user-019", "보리차", false, 144),
            new PointSeed("user-020", "라임소다", false, 121),
            new PointSeed("user-021", "홍차라떼", false, 108),
            new PointSeed("user-022", "아침햇살", false, 96),
            new PointSeed("user-023", "캣닢", false, 84),
            new PointSeed("user-024", "바닐라", false, 72),
            new PointSeed("user-025", "복숭아", false, 61),
            new PointSeed("user-026", "구름빵", false, 48),
            new PointSeed("user-027", "레몬밤", false, 37),
            new PointSeed("user-028", "콩떡", false, 29),
            new PointSeed("user-029", "밤하늘", false, 22),
            new PointSeed("user-030", "민들레", false, 17),
            new PointSeed("user-031", "새싹", false, 9),
            new PointSeed("user-032", "먼지", false, 0)
    );

    private final UserAccountRepository userAccountRepository;
    private final OAuthCredentialRepository oauthCredentialRepository;
    private final CommandRepository commandRepository;
    private final PointLedgerEntryRepository pointLedgerEntryRepository;
    private final PointAdjustmentPresetRepository pointAdjustmentPresetRepository;
    private final WeeklyChatCountRepository weeklyChatCountRepository;
    private final TimerMessageRepository timerMessageRepository;
    private final RouletteConfigRepository rouletteConfigRepository;
    private final RouletteOptionRepository rouletteOptionRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<UserAccount> users = seedUsers();
        UserAccount admin = requireUser(users, LocalTestAccounts.ADMIN_USER_ID);
        seedOAuthCredentials(users);
        seedPointLedger(users, admin);
        seedPointAdjustmentPresets();
        seedWeeklyChatCounts(users);
        seedCommands(admin);
        seedTimerMessages(admin);
        seedRouletteConfig();
        log.info("Local canonical dummy data is ready");
    }

    private List<UserAccount> seedUsers() {
        POINT_SEEDS.stream()
                .filter(seed -> !userAccountRepository.existsById(seed.userId()))
                .map(seed -> UserAccount.builder()
                        .userId(seed.userId())
                        .displayName(seed.displayName())
                        .admin(seed.admin())
                        .build())
                .forEach(userAccountRepository::save);
        userAccountRepository.flush();
        return userAccountRepository.findAllById(POINT_SEEDS.stream().map(PointSeed::userId).toList());
    }

    private void seedOAuthCredentials(List<UserAccount> users) {
        Instant expiresAt = Instant.now().plus(Duration.ofDays(1));
        users.stream()
                .filter(user -> LocalTestAccounts.find(user.getUserId()).isPresent())
                .filter(user -> !oauthCredentialRepository.existsById(user.getUserId()))
                .map(user -> OAuthCredential.builder()
                        .userAccount(user)
                        .accessToken("local-access-token-" + user.getUserId())
                        .refreshToken("local-refresh-token-" + user.getUserId())
                        .tokenType("Bearer")
                        .scope("local")
                        .accessTokenExpiresAt(expiresAt)
                        .build())
                .forEach(oauthCredentialRepository::save);
    }

    private void seedPointLedger(List<UserAccount> users, UserAccount admin) {
        POINT_SEEDS.stream()
                .filter(seed -> seed.point() != 0)
                .filter(seed -> !pointLedgerEntryRepository.existsByIdempotencyKey("local-opening-" + seed.userId()))
                .forEach(seed -> pointLedgerEntryRepository.save(PointLedgerEntry.builder()
                        .userAccount(requireUser(users, seed.userId()))
                        .delta(seed.point())
                        .sourceType(PointSourceType.ADMIN_ADJUSTMENT)
                        .sourceReference("local-dummy")
                        .description("로컬 초기 포인트")
                        .privateNote("canonical local fixture")
                        .actorUser(admin)
                        .idempotencyKey("local-opening-" + seed.userId())
                        .build()));
    }

    private void seedPointAdjustmentPresets() {
        if (pointAdjustmentPresetRepository.count() > 0) {
            return;
        }
        pointAdjustmentPresetRepository.saveAll(List.of(
                preset(100, "호감도 보너스"),
                preset(10, "소소한 보너스"),
                preset(-5, "업보 차감"),
                preset(-50, "경고 차감"),
                preset(1, "정정 +1")
        ));
    }

    private void seedWeeklyChatCounts(List<UserAccount> users) {
        if (weeklyChatCountRepository.count() > 0) {
            return;
        }
        LocalDate weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        weeklyChatCountRepository.saveAll(List.of(
                weeklyCount(weekStart, requireUser(users, "user-001"), 421),
                weeklyCount(weekStart, requireUser(users, "user-002"), 387),
                weeklyCount(weekStart, requireUser(users, "user-003"), 244),
                weeklyCount(weekStart, requireUser(users, "user-004"), 199),
                weeklyCount(weekStart, requireUser(users, "user-005"), 153),
                weeklyCount(weekStart, requireUser(users, LocalTestAccounts.VIEWER_USER_ID), 128)
        ));
    }

    private void seedCommands(UserAccount admin) {
        if (commandRepository.count() > 0) {
            return;
        }
        commandRepository.saveAll(List.of(
                Command.builder()
                        .triggerToken("!출석")
                        .messageTemplate("{viewer.nickname}님, {streak.current}일 연속 출석! 오늘은 {count.user}번째 출석입니다.")
                        .active(true)
                        .executionPolicy(CommandExecutionPolicy.USER_CALENDAR_DAY)
                        .createdByUser(admin)
                        .updatedByUser(admin)
                        .build(),
                Command.builder()
                        .triggerToken("!인성")
                        .messageTemplate("{viewer.nickname}님의 {count.user}번째 인성질 · 전체 {count.total}회")
                        .active(true)
                        .executionPolicy(CommandExecutionPolicy.USER_INTERVAL)
                        .userCooldownSeconds(30)
                        .createdByUser(admin)
                        .updatedByUser(admin)
                        .build()
        ));
    }

    private void seedTimerMessages(UserAccount admin) {
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
                .createdByUser(admin)
                .updatedByUser(admin)
                .build());
    }

    private void seedRouletteConfig() {
        if (rouletteConfigRepository.count() > 0) {
            return;
        }
        Instant now = Instant.now();
        RouletteConfig config = rouletteConfigRepository.saveAndFlush(RouletteConfig.builder()
                .title("로컬 테스트 룰렛")
                .triggerToken("!룰렛")
                .pricePerRound(1_000L)
                .highRoundThreshold(50)
                .status(RouletteConfigStatus.DRAFT)
                .createdAt(now)
                .updatedAt(now)
                .build());
        rouletteOptionRepository.saveAll(List.of(
                option(config, "호감도 +100", 5_000, false, RewardType.POINT, ConversionMode.AUTO, 100L, 1, now),
                option(config, "미션권", 2_000, false, RewardType.MISSION, ConversionMode.MANUAL, null, 2, now),
                option(config, "다음 기회에", 3_000, true, RewardType.CUSTOM, ConversionMode.NONE, null, 3, now)
        ));
        rouletteOptionRepository.flush();
        config.activate(Instant.now());
        rouletteConfigRepository.saveAndFlush(config);
    }

    private PointAdjustmentPreset preset(long amount, String label) {
        return PointAdjustmentPreset.builder().amount(amount).label(label).build();
    }

    private WeeklyChatCount weeklyCount(LocalDate weekStart, UserAccount user, long count) {
        return WeeklyChatCount.builder()
                .weekStartDate(weekStart)
                .userAccount(user)
                .chatCount(count)
                .build();
    }

    private RouletteOption option(
            RouletteConfig config,
            String label,
            int probability,
            boolean losing,
            RewardType rewardType,
            ConversionMode conversionMode,
            Long pointDelta,
            int displayOrder,
            Instant createdAt
    ) {
        return RouletteOption.builder()
                .rouletteConfig(config)
                .label(label)
                .probabilityBasisPoints(probability)
                .losing(losing)
                .rewardType(rewardType)
                .conversionMode(conversionMode)
                .pointDelta(pointDelta)
                .displayOrder(displayOrder)
                .createdAt(createdAt)
                .build();
    }

    private UserAccount requireUser(List<UserAccount> users, String userId) {
        return users.stream()
                .filter(user -> user.getUserId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("local user not found: " + userId));
    }

    private record PointSeed(String userId, String displayName, boolean admin, long point) {
    }
}
