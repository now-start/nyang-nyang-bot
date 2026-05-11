package org.nowstart.nyangnyangbot.application.service.favorite;

import org.nowstart.nyangnyangbot.application.port.in.favorite.dto.FavoriteLedgerResult;
import org.nowstart.nyangnyangbot.application.port.in.favorite.dto.AdjustFavoriteCommand;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nowstart.nyangnyangbot.application.port.out.favorite.repository.CheckIdempotencyPort;
import org.nowstart.nyangnyangbot.application.port.out.favorite.repository.LoadFavoriteAccountPort;
import org.nowstart.nyangnyangbot.application.port.out.favorite.repository.SaveFavoriteAccountPort;
import org.nowstart.nyangnyangbot.application.port.out.favorite.repository.SaveFavoriteLedgerPort;
import org.nowstart.nyangnyangbot.domain.favorite.FavoriteAccount;
import org.nowstart.nyangnyangbot.domain.favorite.FavoriteLedgerEntry;
import org.nowstart.nyangnyangbot.domain.favorite.FavoriteSourceType;

class FavoriteLedgerServiceTest {

    private FakeFavoritePorts ports;
    private FavoriteLedgerService service;

    @BeforeEach
    void setUp() {
        ports = new FakeFavoritePorts();
        service = new FavoriteLedgerService(ports, ports, ports, ports);
    }

    @Test
    void adjust_ShouldSaveAccountAndLedgerInSingleUseCase() {
        ports.account = FavoriteAccount.of("user-1", "이전닉", 10);

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

        assertThat(result.beforeBalance()).isEqualTo(10);
        assertThat(result.delta()).isEqualTo(7);
        assertThat(result.afterBalance()).isEqualTo(17);
        assertThat(result.duplicate()).isFalse();
        assertThat(ports.savedAccounts).hasSize(1);
        assertThat(ports.savedAccounts.getFirst().getNickName()).isEqualTo("새닉");
        assertThat(ports.savedLedgers).hasSize(1);
        assertThat(ports.savedLedgers.getFirst().balanceAfter()).isEqualTo(17);
        assertThat(ports.savedLedgers.getFirst().sourceType()).isEqualTo(FavoriteSourceType.ADMIN_ADJUSTMENT);
        assertThat(ports.savedLedgers.getFirst().nickNameSnapshot()).isEqualTo("새닉");
    }

    @Test
    void adjust_ShouldSkipDuplicateIdempotencyKey() {
        ports.account = FavoriteAccount.of("user-1", "치즈냥", 10);
        ports.existingIdempotencyKey = "dup-key";

        FavoriteLedgerResult result = service.adjust(AdjustFavoriteCommand.builder()
                .userId("user-1")
                .delta(5)
                .sourceType(FavoriteSourceType.ATTENDANCE)
                .publicDescription("출석체크")
                .idempotencyKey("dup-key")
                .createIfMissing(false)
                .build());

        assertThat(result.duplicate()).isTrue();
        assertThat(ports.account.getBalance()).isEqualTo(10);
        assertThat(ports.savedAccounts).isEmpty();
        assertThat(ports.savedLedgers).isEmpty();
    }

    @Test
    void adjust_ShouldCreateMissingAccount_WhenRequested() {
        FavoriteLedgerResult result = service.adjust(AdjustFavoriteCommand.builder()
                .userId("user-new")
                .nickName("신규")
                .delta(3)
                .sourceType(FavoriteSourceType.ATTENDANCE)
                .publicDescription("출석체크")
                .createIfMissing(true)
                .build());

        assertThat(result.beforeBalance()).isZero();
        assertThat(result.afterBalance()).isEqualTo(3);
        assertThat(ports.savedAccounts.getFirst().getUserId()).isEqualTo("user-new");
        assertThat(ports.savedLedgers.getFirst().delta()).isEqualTo(3);
    }

    @Test
    void adjust_ShouldRejectMissingAccount_WhenCreationIsNotAllowed() {
        assertThatThrownBy(() -> service.adjust(AdjustFavoriteCommand.builder()
                .userId("missing")
                .delta(3)
                .sourceType(FavoriteSourceType.ADMIN_ADJUSTMENT)
                .publicDescription("관리자 조정")
                .createIfMissing(false)
                .build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Favorite user not found");
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
}
