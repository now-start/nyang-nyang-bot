package org.nowstart.nyangnyangbot.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.adapter.out.persistence.command.repository.CommandRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.point.entity.PointLedgerEntry;
import org.nowstart.nyangnyangbot.adapter.out.persistence.point.repository.PointAdjustmentPresetRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.point.repository.PointLedgerEntryRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.entity.RouletteConfig;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.repository.RouletteConfigRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.repository.RouletteOptionRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.timer.repository.TimerMessageRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.user.entity.UserAccount;
import org.nowstart.nyangnyangbot.adapter.out.persistence.user.repository.OAuthCredentialRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.user.repository.UserAccountRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.weekly.repository.WeeklyChatCountRepository;
import org.springframework.boot.ApplicationArguments;

@ExtendWith(MockitoExtension.class)
class LocalDummyDataInitializerTest {

    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private OAuthCredentialRepository oauthCredentialRepository;
    @Mock
    private CommandRepository commandRepository;
    @Mock
    private PointLedgerEntryRepository pointLedgerEntryRepository;
    @Mock
    private PointAdjustmentPresetRepository pointAdjustmentPresetRepository;
    @Mock
    private WeeklyChatCountRepository weeklyChatCountRepository;
    @Mock
    private TimerMessageRepository timerMessageRepository;
    @Mock
    private RouletteConfigRepository rouletteConfigRepository;
    @Mock
    private RouletteOptionRepository rouletteOptionRepository;
    @Mock
    private ApplicationArguments applicationArguments;

    private List<UserAccount> users;

    @BeforeEach
    void setUp() {
        users = List.of(
                user(LocalTestAccounts.ADMIN_USER_ID, "로컬 관리자", true),
                user(LocalTestAccounts.VIEWER_USER_ID, "일반 시청자", false),
                user(LocalTestAccounts.NEGATIVE_USER_ID, "이불밖은위험해", false),
                user("user-001", "치즈냥", false),
                user("user-002", "새벽라떼", false),
                user("user-003", "민트초코", false),
                user("user-004", "고구마", false),
                user("user-005", "달토끼", false),
                user("user-007", "파란별", false),
                user("user-008", "츄르도둑", false),
                user("user-009", "우주고양이", false),
                user("user-010", "밤톨이", false),
                user("user-011", "시금치파스타", false),
                user("user-012", "감자탕수육", false),
                user("user-013", "솜사탕", false),
                user("user-014", "유자차", false),
                user("user-015", "별사탕", false),
                user("user-016", "모찌", false),
                user("user-017", "초코칩", false),
                user("user-018", "귤냥이", false),
                user("user-019", "보리차", false),
                user("user-020", "라임소다", false),
                user("user-021", "홍차라떼", false),
                user("user-022", "아침햇살", false),
                user("user-023", "캣닢", false),
                user("user-024", "바닐라", false),
                user("user-025", "복숭아", false),
                user("user-026", "구름빵", false),
                user("user-027", "레몬밤", false),
                user("user-028", "콩떡", false),
                user("user-029", "밤하늘", false),
                user("user-030", "민들레", false),
                user("user-031", "새싹", false),
                user("user-032", "먼지", false)
        );
        given(userAccountRepository.findAllById(any())).willReturn(users);
    }

    @Test
    void run_ShouldSeedCanonicalFixturesWhenRepositoriesAreEmpty() {
        given(userAccountRepository.save(any(UserAccount.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(pointLedgerEntryRepository.save(any(PointLedgerEntry.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(rouletteConfigRepository.saveAndFlush(any(RouletteConfig.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(userAccountRepository.existsById(anyString())).willReturn(false);
        given(oauthCredentialRepository.existsById(anyString())).willReturn(false);
        given(pointLedgerEntryRepository.existsByIdempotencyKey(anyString())).willReturn(false);
        given(pointAdjustmentPresetRepository.count()).willReturn(0L);
        given(weeklyChatCountRepository.count()).willReturn(0L);
        given(commandRepository.count()).willReturn(0L);
        given(timerMessageRepository.count()).willReturn(0L);
        given(rouletteConfigRepository.count()).willReturn(0L);

        initializer().run(applicationArguments);

        BDDMockito.then(userAccountRepository).should(atLeastOnce()).save(any(UserAccount.class));
        BDDMockito.then(oauthCredentialRepository).should(atLeastOnce()).save(any());
        BDDMockito.then(pointLedgerEntryRepository).should(atLeastOnce()).save(any(PointLedgerEntry.class));
        BDDMockito.then(pointAdjustmentPresetRepository).should().saveAll(any());
        BDDMockito.then(weeklyChatCountRepository).should().saveAll(any());
        BDDMockito.then(commandRepository).should().saveAll(any());
        BDDMockito.then(timerMessageRepository).should().save(any());
        BDDMockito.then(rouletteOptionRepository).should().saveAll(any());
        BDDMockito.then(rouletteConfigRepository).should(BDDMockito.times(2)).saveAndFlush(any());
    }

    @Test
    void run_ShouldNotDuplicateExistingCanonicalFixtures() {
        given(userAccountRepository.existsById(anyString())).willReturn(true);
        given(oauthCredentialRepository.existsById(anyString())).willReturn(true);
        given(pointLedgerEntryRepository.existsByIdempotencyKey(anyString())).willReturn(true);
        given(pointAdjustmentPresetRepository.count()).willReturn(1L);
        given(weeklyChatCountRepository.count()).willReturn(1L);
        given(commandRepository.count()).willReturn(1L);
        given(timerMessageRepository.count()).willReturn(1L);
        given(rouletteConfigRepository.count()).willReturn(1L);

        initializer().run(applicationArguments);

        BDDMockito.then(userAccountRepository).should(never()).save(any(UserAccount.class));
        BDDMockito.then(oauthCredentialRepository).should(never()).save(any());
        BDDMockito.then(pointLedgerEntryRepository).should(never()).save(any(PointLedgerEntry.class));
        BDDMockito.then(pointAdjustmentPresetRepository).should(never()).saveAll(any());
        BDDMockito.then(weeklyChatCountRepository).should(never()).saveAll(any());
        BDDMockito.then(commandRepository).should(never()).saveAll(any());
        BDDMockito.then(timerMessageRepository).should(never()).save(any());
        BDDMockito.then(rouletteOptionRepository).should(never()).saveAll(any());
        BDDMockito.then(rouletteConfigRepository).should(never()).saveAndFlush(any());
    }

    private LocalDummyDataInitializer initializer() {
        return new LocalDummyDataInitializer(
                userAccountRepository,
                oauthCredentialRepository,
                commandRepository,
                pointLedgerEntryRepository,
                pointAdjustmentPresetRepository,
                weeklyChatCountRepository,
                timerMessageRepository,
                rouletteConfigRepository,
                rouletteOptionRepository
        );
    }

    private UserAccount user(String userId, String displayName, boolean admin) {
        return UserAccount.builder()
                .userId(userId)
                .displayName(displayName)
                .admin(admin)
                .build();
    }
}
