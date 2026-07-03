package org.nowstart.nyangnyangbot.application.service.favorite;

import org.nowstart.nyangnyangbot.application.port.in.favorite.AdjustFavoriteUseCase.FavoriteLedgerResult;
import org.nowstart.nyangnyangbot.application.port.in.favorite.AdjustFavoriteUseCase.AdjustFavoriteCommand;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

import jakarta.validation.Validation;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nowstart.nyangnyangbot.application.port.out.favorite.CheckIdempotencyPort;
import org.nowstart.nyangnyangbot.application.port.out.favorite.LoadFavoriteAccountPort;
import org.nowstart.nyangnyangbot.application.port.out.favorite.SaveFavoriteAccountPort;
import org.nowstart.nyangnyangbot.application.port.out.favorite.SaveFavoriteLedgerPort;
import org.nowstart.nyangnyangbot.application.validation.UseCaseValidator;
import org.nowstart.nyangnyangbot.domain.favorite.FavoriteAccount;
import org.nowstart.nyangnyangbot.domain.favorite.FavoriteLedgerEntry;
import org.nowstart.nyangnyangbot.domain.favorite.FavoriteSourceType;

class FavoriteLedgerServiceTest {

    private FakeFavoritePorts ports;
    private FavoriteLedgerService service;

    @BeforeEach
    void setUp() {
        ports = new FakeFavoritePorts();
        service = new FavoriteLedgerService(ports, ports, ports, ports, validator());
    }

    @Test
    void adjust_ShouldSaveAccountAndLedgerInSingleUseCase() {
        // 준비
        ports.account = FavoriteAccount.of("user-1", "이전닉", 10);

        // 실행
        FavoriteLedgerResult result = service.adjust(AdjustFavoriteCommand.builder()
                .userId("user-1")
                .nickName("새닉")
                .delta(7)
                .sourceType(FavoriteSourceType.ADMIN_ADJUSTMENT)
                .sourceId("adjustment:1")
                .publicDescription("관리자 조정")
                .actorId("admin-1")
                .idempotencyKey("admin:user-1:1")
                .allowNegativeBalance(true)
                .createIfMissing(false)
                .build());

        // 검증
        then(result.beforeBalance()).isEqualTo(10);
        then(result.delta()).isEqualTo(7);
        then(result.afterBalance()).isEqualTo(17);
        then(result.duplicate()).isFalse();
        then(ports.savedAccounts).hasSize(1);
        then(ports.savedAccounts.getFirst().getNickName()).isEqualTo("새닉");
        then(ports.savedLedgers).hasSize(1);
        then(ports.savedLedgers.getFirst().balanceAfter()).isEqualTo(17);
        then(ports.savedLedgers.getFirst().sourceType()).isEqualTo(FavoriteSourceType.ADMIN_ADJUSTMENT);
        then(ports.savedLedgers.getFirst().nickNameSnapshot()).isEqualTo("새닉");
    }

    @Test
    void adjust_ShouldSkipDuplicateIdempotencyKey() {
        // 준비
        ports.account = FavoriteAccount.of("user-1", "치즈냥", 10);
        ports.existingIdempotencyKey = "dup-key";

        // 실행
        FavoriteLedgerResult result = service.adjust(AdjustFavoriteCommand.builder()
                .userId("user-1")
                .delta(5)
                .sourceType(FavoriteSourceType.ATTENDANCE)
                .publicDescription("출석체크")
                .idempotencyKey("dup-key")
                .createIfMissing(false)
                .build());

        // 검증
        then(result.duplicate()).isTrue();
        then(ports.account.getBalance()).isEqualTo(10);
        then(ports.savedAccounts).isEmpty();
        then(ports.savedLedgers).isEmpty();
    }

    @Test
    void adjust_ShouldCreateMissingAccount_WhenRequested() {
        // 실행
        FavoriteLedgerResult result = service.adjust(AdjustFavoriteCommand.builder()
                .userId("user-new")
                .nickName("신규")
                .delta(3)
                .sourceType(FavoriteSourceType.ATTENDANCE)
                .publicDescription("출석체크")
                .createIfMissing(true)
                .build());

        // 검증
        then(result.beforeBalance()).isZero();
        then(result.afterBalance()).isEqualTo(3);
        then(ports.savedAccounts.getFirst().getUserId()).isEqualTo("user-new");
        then(ports.savedLedgers.getFirst().delta()).isEqualTo(3);
    }

    @Test
    void adjust_ShouldRejectMissingAccount_WhenCreationIsNotAllowed() {
        // 실행 및 검증
        thenThrownBy(() -> service.adjust(AdjustFavoriteCommand.builder()
                .userId("missing")
                .delta(3)
                .sourceType(FavoriteSourceType.ADMIN_ADJUSTMENT)
                .publicDescription("관리자 조정")
                .createIfMissing(false)
                .build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Favorite user not found");
    }

    @Test
    void correct_ShouldForceCorrectionSourceAndAllowNegativeBalance() {
        // 준비
        ports.account = FavoriteAccount.of("user-1", "치즈냥", 3);

        // 실행
        FavoriteLedgerResult result = service.correct(AdjustFavoriteCommand.builder()
                .userId("user-1")
                .nickName("정정냥")
                .delta(-5)
                .sourceType(FavoriteSourceType.ADMIN_ADJUSTMENT)
                .correctionOfLedgerId(10L)
                .actorId("admin-1")
                .build());

        // 검증
        then(result.afterBalance()).isEqualTo(-2);
        then(result.history()).isEqualTo("호감도 정정");
        then(ports.savedLedgers.getFirst().sourceType()).isEqualTo(FavoriteSourceType.CORRECTION);
        then(ports.savedLedgers.getFirst().correctionOfLedgerId()).isEqualTo(10L);
    }

    @Test
    void adjust_ShouldUseDefaultHistoryForEachSourceType() {
        // 준비
        FavoriteSourceType[] sourceTypes = {
                FavoriteSourceType.ATTENDANCE,
                FavoriteSourceType.SHEET_MIGRATION,
                FavoriteSourceType.ADMIN_ADJUSTMENT,
                FavoriteSourceType.UPBO_MANUAL,
                FavoriteSourceType.UPBO_ROULETTE
        };
        String[] histories = {"출석체크", "데이터 동기화", "관리자 조정", "업보 적용", "룰렛 결과"};

        for (int i = 0; i < sourceTypes.length; i++) {
            ports.account = FavoriteAccount.of("user-" + i, "치즈냥", 0);

            // 실행
            FavoriteLedgerResult result = service.adjust(AdjustFavoriteCommand.builder()
                    .userId("user-" + i)
                    .delta(1)
                    .sourceType(sourceTypes[i])
                    .createIfMissing(false)
                    .build());

            // 검증
            then(result.history()).isEqualTo(histories[i]);
        }
    }

    @Test
    void adjust_ShouldRejectInvalidCommandFields() {
        // 실행 및 검증
        thenThrownBy(() -> service.adjust(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("command is required");
        thenThrownBy(() -> service.adjust(AdjustFavoriteCommand.builder()
                .delta(1)
                .sourceType(FavoriteSourceType.ATTENDANCE)
                .build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("userId is required");
        thenThrownBy(() -> service.adjust(AdjustFavoriteCommand.builder()
                .userId("user-1")
                .sourceType(FavoriteSourceType.ATTENDANCE)
                .build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("delta must not be zero");
        thenThrownBy(() -> service.adjust(AdjustFavoriteCommand.builder()
                .userId("user-1")
                .delta(1)
                .build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("sourceType is required");
    }

    private static class FakeFavoritePorts implements LoadFavoriteAccountPort, SaveFavoriteAccountPort,
            SaveFavoriteLedgerPort, CheckIdempotencyPort {

        private FavoriteAccount account;
        private String existingIdempotencyKey;
        private final List<FavoriteAccount> savedAccounts = new ArrayList<>();
        private final List<FavoriteLedgerEntry> savedLedgers = new ArrayList<>();

        @Override
        public Optional<FavoriteAccount> loadForUpdate(String userId) {
            if (account == null || !account.getUserId().equals(userId)) {
                return Optional.empty();
            }
            return Optional.of(account);
        }

        @Override
        public FavoriteAccount save(FavoriteAccount account) {
            this.account = account;
            savedAccounts.add(account);
            return account;
        }

        @Override
        public FavoriteLedgerEntry save(FavoriteLedgerEntry ledgerEntry) {
            savedLedgers.add(ledgerEntry);
            return ledgerEntry;
        }

        @Override
        public boolean existsByIdempotencyKey(String idempotencyKey) {
            return idempotencyKey != null && idempotencyKey.equals(existingIdempotencyKey);
        }
    }

    private UseCaseValidator validator() {
        return new UseCaseValidator(Validation.buildDefaultValidatorFactory().getValidator());
    }
}
