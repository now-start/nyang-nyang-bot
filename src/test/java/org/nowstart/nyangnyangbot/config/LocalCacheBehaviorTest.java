package org.nowstart.nyangnyangbot.config;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.nowstart.nyangnyangbot.support.OutboundContractTestSupport.outboundContractValidator;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.nowstart.nyangnyangbot.adapter.out.persistence.command.CommandPersistenceAdapter;
import org.nowstart.nyangnyangbot.adapter.out.persistence.command.entity.Command;
import org.nowstart.nyangnyangbot.adapter.out.persistence.command.repository.CommandRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.point.PointAdjustmentPresetPersistenceAdapter;
import org.nowstart.nyangnyangbot.adapter.out.persistence.point.entity.PointAdjustmentPreset;
import org.nowstart.nyangnyangbot.adapter.out.persistence.point.repository.PointAdjustmentPresetRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.user.repository.UserAccountRepository;
import org.nowstart.nyangnyangbot.adapter.out.validation.OutboundContractValidator;
import org.nowstart.nyangnyangbot.application.port.out.command.CommandPort;
import org.nowstart.nyangnyangbot.application.port.out.point.PointAdjustmentPresetPort;
import org.nowstart.nyangnyangbot.config.cache.CacheConfig;
import org.nowstart.nyangnyangbot.config.cache.CacheNames;
import org.nowstart.nyangnyangbot.domain.command.CommandExecutionPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(classes = {
        CacheConfig.class,
        LocalCacheBehaviorTest.TestConfig.class
})
class LocalCacheBehaviorTest {

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private PointAdjustmentPresetPort pointAdjustmentPresetPort;

    @Autowired
    private CommandPort commandPort;

    @Autowired
    private PointAdjustmentPresetRepository pointAdjustmentPresetRepository;

    @Autowired
    private CommandRepository commandRepository;

    @BeforeEach
    void setUp() {
        cacheManager.getCacheNames()
                .forEach(name -> Objects.requireNonNull(cacheManager.getCache(name)).clear());
        BDDMockito.reset(pointAdjustmentPresetRepository, commandRepository);
    }

    @Test
    void localCaches_ShouldRecordStatisticsForActuatorMetrics() {
        CacheNames.ALL.forEach(name -> {
            org.springframework.cache.Cache cache = Objects.requireNonNull(cacheManager.getCache(name));
            Object nativeCache = cache.getNativeCache();

            if (nativeCache instanceof com.github.benmanes.caffeine.cache.Cache<?, ?> caffeineCache) {
                then(caffeineCache.policy().isRecordingStats()).isTrue();
                return;
            }
            throw new AssertionError("Expected Caffeine cache for " + name + " but was " + nativeCache.getClass());
        });
    }

    @Test
    void pointAdjustmentPresets_ShouldCacheListAndEvictWhenSaved() {
        PointAdjustmentPreset first = preset(1L, 10, "보너스");
        PointAdjustmentPreset second = preset(2L, 20, "추가 보너스");
        given(pointAdjustmentPresetRepository.findAll())
                .willReturn(List.of(first))
                .willReturn(List.of(second));
        given(pointAdjustmentPresetRepository.save(any(PointAdjustmentPreset.class))).willReturn(second);

        then(pointAdjustmentPresetPort.findAll().getFirst().id()).isEqualTo(1L);
        then(pointAdjustmentPresetPort.findAll().getFirst().id()).isEqualTo(1L);
        BDDMockito.then(pointAdjustmentPresetRepository).should(times(1)).findAll();

        pointAdjustmentPresetPort.save(20, "추가 보너스");

        then(pointAdjustmentPresetPort.findAll().getFirst().id()).isEqualTo(2L);
        BDDMockito.then(pointAdjustmentPresetRepository).should(times(2)).findAll();
    }

    @Test
    void commandActiveTrigger_ShouldCacheAndEvictWhenUpdated() {
        Command first = command(1L, "!호감도");
        Command second = command(2L, "!호감도");
        given(commandRepository.findByActiveTrue())
                .willReturn(List.of(first))
                .willReturn(List.of(second));
        given(commandRepository.findByIdForUpdate(1L)).willReturn(Optional.of(first));

        then(commandPort.findActiveCommandsByTrigger().get("!호감도").id()).isEqualTo(1L);
        then(commandPort.findActiveCommandsByTrigger().get("!호감도").id()).isEqualTo(1L);
        BDDMockito.then(commandRepository).should(times(1)).findByActiveTrue();

        commandPort.update(new CommandPort.UpdateData(
                1L,
                "!호감도",
                "{viewer.nickname}님의 호감도는 {point.balance} 입니다.💛",
                true,
                30,
                "system"
        ));

        then(commandPort.findActiveCommandsByTrigger().get("!호감도").id()).isEqualTo(2L);
        BDDMockito.then(commandRepository).should(times(2)).findByActiveTrue();
    }

    @Test
    void commandActiveTriggers_ShouldCacheEmptyCatalog() {
        given(commandRepository.findByActiveTrue()).willReturn(List.of());

        then(commandPort.findActiveCommandsByTrigger()).isEmpty();
        then(commandPort.findActiveCommandsByTrigger()).isEmpty();
        BDDMockito.then(commandRepository).should(times(1)).findByActiveTrue();
    }

    private PointAdjustmentPreset preset(Long id, long amount, String label) {
        return PointAdjustmentPreset.builder()
                .id(id)
                .amount(amount)
                .label(label)
                .build();
    }

    private Command command(Long id, String trigger) {
        return Command.builder()
                .id(id)
                .triggerToken(trigger)
                .messageTemplate("{viewer.nickname}님의 호감도는 {point.balance} 입니다.💛")
                .active(true)
                .executionPolicy(CommandExecutionPolicy.USER_INTERVAL)
                .userCooldownSeconds(30)
                .build();
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        OutboundContractValidator outboundContractValidatorBean() {
            return outboundContractValidator();
        }

        @Bean
        CommandPort commandPort(
                CommandRepository commandRepository,
                UserAccountRepository userAccountRepository,
                OutboundContractValidator contractValidator
        ) {
            return new CommandPersistenceAdapter(commandRepository, userAccountRepository, contractValidator);
        }

        @Bean
        PointAdjustmentPresetPort pointAdjustmentPresetPort(
                PointAdjustmentPresetRepository pointAdjustmentPresetRepository
        ) {
            return new PointAdjustmentPresetPersistenceAdapter(pointAdjustmentPresetRepository);
        }

        @Bean
        CommandRepository commandRepository() {
            return BDDMockito.mock(CommandRepository.class);
        }

        @Bean
        UserAccountRepository userAccountRepository() {
            return BDDMockito.mock(UserAccountRepository.class);
        }

        @Bean
        PointAdjustmentPresetRepository pointAdjustmentPresetRepository() {
            return BDDMockito.mock(PointAdjustmentPresetRepository.class);
        }
    }
}
