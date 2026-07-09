package org.nowstart.nyangnyangbot.config;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.BDDMockito;
import org.nowstart.nyangnyangbot.adapter.out.persistence.command.CommandPersistenceAdapter;
import org.nowstart.nyangnyangbot.adapter.out.persistence.command.CommandPersistenceMapper;
import org.nowstart.nyangnyangbot.adapter.out.persistence.command.entity.Command;
import org.nowstart.nyangnyangbot.adapter.out.persistence.command.repository.CommandRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.favorite.FavoriteAdjustmentPersistenceAdapter;
import org.nowstart.nyangnyangbot.adapter.out.persistence.favorite.FavoriteAdjustmentPersistenceMapper;
import org.nowstart.nyangnyangbot.adapter.out.persistence.favorite.entity.FavoriteAdjustment;
import org.nowstart.nyangnyangbot.adapter.out.persistence.favorite.repository.FavoriteAdjustmentRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.RoulettePersistenceAdapter;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.RoulettePersistenceMapper;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.entity.RouletteItem;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.entity.RouletteTable;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.repository.RouletteEventRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.repository.RouletteItemRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.repository.RouletteRoundResultRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.repository.RouletteTableRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.upbo.UpboPersistenceAdapter;
import org.nowstart.nyangnyangbot.adapter.out.persistence.upbo.UpboPersistenceMapper;
import org.nowstart.nyangnyangbot.adapter.out.persistence.upbo.entity.UpboTemplate;
import org.nowstart.nyangnyangbot.adapter.out.persistence.upbo.repository.UpboTemplateRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.upbo.repository.UserUpboRepository;
import org.nowstart.nyangnyangbot.application.port.out.command.CommandPort;
import org.nowstart.nyangnyangbot.application.port.out.favorite.FavoriteAdjustmentPort;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort;
import org.nowstart.nyangnyangbot.application.port.out.upbo.UpboPort;
import org.nowstart.nyangnyangbot.config.cache.CacheConfig;
import org.nowstart.nyangnyangbot.config.cache.CacheNames;
import org.nowstart.nyangnyangbot.domain.type.CommandActionKey;
import org.nowstart.nyangnyangbot.domain.type.CommandType;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
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
    private FavoriteAdjustmentPort favoriteAdjustmentPort;

    @Autowired
    private UpboPort upboPort;

    @Autowired
    private RoulettePort roulettePort;

    @Autowired
    private CommandPort commandPort;

    @Autowired
    private FavoriteAdjustmentRepository favoriteAdjustmentRepository;

    @Autowired
    private UpboTemplateRepository upboTemplateRepository;

    @Autowired
    private RouletteTableRepository rouletteTableRepository;

    @Autowired
    private RouletteItemRepository rouletteItemRepository;

    @Autowired
    private CommandRepository commandRepository;

    @BeforeEach
    void setUp() {
        cacheManager.getCacheNames()
                .forEach(name -> Objects.requireNonNull(cacheManager.getCache(name)).clear());
        BDDMockito.reset(
                favoriteAdjustmentRepository,
                upboTemplateRepository,
                rouletteTableRepository,
                rouletteItemRepository,
                commandRepository
        );
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
    void favoriteAdjustments_ShouldCacheListAndEvictWhenSaved() {
        FavoriteAdjustment first = adjustment(1L, 10, "보너스");
        FavoriteAdjustment second = adjustment(2L, 20, "추가 보너스");
        given(favoriteAdjustmentRepository.findAll())
                .willReturn(List.of(first))
                .willReturn(List.of(second));
        given(favoriteAdjustmentRepository.save(any(FavoriteAdjustment.class))).willReturn(second);

        then(favoriteAdjustmentPort.findAll().getFirst().id()).isEqualTo(1L);
        then(favoriteAdjustmentPort.findAll().getFirst().id()).isEqualTo(1L);
        BDDMockito.then(favoriteAdjustmentRepository).should(times(1)).findAll();

        favoriteAdjustmentPort.save(20, "추가 보너스");

        then(favoriteAdjustmentPort.findAll().getFirst().id()).isEqualTo(2L);
        BDDMockito.then(favoriteAdjustmentRepository).should(times(2)).findAll();
    }

    @Test
    void upboTemplates_ShouldCacheActiveTemplatesAndEvictWhenCreated() {
        UpboTemplate first = template(1L, "호감도 +100");
        UpboTemplate second = template(2L, "호감도 +200");
        given(upboTemplateRepository.findByActiveTrueOrderByDisplayOrderAscIdAsc())
                .willReturn(List.of(first))
                .willReturn(List.of(second));
        given(upboTemplateRepository.save(any(UpboTemplate.class))).willReturn(second);

        then(upboPort.findActiveTemplates().getFirst().id()).isEqualTo(1L);
        then(upboPort.findActiveTemplates().getFirst().id()).isEqualTo(1L);
        BDDMockito.then(upboTemplateRepository).should(times(1)).findByActiveTrueOrderByDisplayOrderAscIdAsc();

        upboPort.createTemplate("호감도 +200", "설명", 2, 200, RewardType.FAVORITE, ConversionMode.AUTO);

        then(upboPort.findActiveTemplates().getFirst().id()).isEqualTo(2L);
        BDDMockito.then(upboTemplateRepository).should(times(2)).findByActiveTrueOrderByDisplayOrderAscIdAsc();
    }

    @Test
    void rouletteActiveConfiguration_ShouldCacheAndEvictOnItemChange() {
        RouletteTable table = table(1L, true);
        RouletteItem first = item(10L, table, "꽝");
        RouletteItem second = item(11L, table, "당첨");
        given(rouletteTableRepository.findById(1L)).willReturn(Optional.of(table));
        given(rouletteItemRepository.findByRouletteTableIdAndActiveTrueOrderByDisplayOrderAscIdAsc(1L))
                .willReturn(List.of(first))
                .willReturn(List.of(second));
        given(rouletteItemRepository.save(any(RouletteItem.class))).willReturn(second);

        then(roulettePort.findActiveItemsByTableId(1L).getFirst().id()).isEqualTo(10L);
        then(roulettePort.findActiveItemsByTableId(1L).getFirst().id()).isEqualTo(10L);
        BDDMockito.then(rouletteItemRepository)
                .should(times(1))
                .findByRouletteTableIdAndActiveTrueOrderByDisplayOrderAscIdAsc(1L);

        roulettePort.addItem(1L, "당첨", 9_000, false, RewardType.FAVORITE, ConversionMode.AUTO, 100, 2);

        then(roulettePort.findActiveItemsByTableId(1L).getFirst().id()).isEqualTo(11L);
        BDDMockito.then(rouletteItemRepository)
                .should(times(2))
                .findByRouletteTableIdAndActiveTrueOrderByDisplayOrderAscIdAsc(1L);
    }

    @Test
    void rouletteTableById_ShouldNotCacheMissingTable() {
        given(rouletteTableRepository.findById(1L)).willReturn(Optional.empty());

        then(roulettePort.findTableById(1L)).isEmpty();
        then(roulettePort.findTableById(1L)).isEmpty();
        BDDMockito.then(rouletteTableRepository).should(times(2)).findById(1L);
    }

    @Test
    void rouletteLatestActiveTable_ShouldCacheAndEvictWhenActivationChanges() {
        RouletteTable table = table(1L, false);
        given(rouletteTableRepository.findFirstByActiveTrueOrderByIdDesc())
                .willReturn(Optional.of(table));
        given(rouletteTableRepository.findById(1L)).willReturn(Optional.of(table));
        given(rouletteTableRepository.findByActiveTrue()).willReturn(List.of(table));
        given(rouletteTableRepository.save(table)).willReturn(table);

        then(roulettePort.findLatestActiveTable()).isPresent();
        then(roulettePort.findLatestActiveTable()).isPresent();
        BDDMockito.then(rouletteTableRepository).should(times(1)).findFirstByActiveTrueOrderByIdDesc();

        roulettePort.activateTable(1L);

        then(roulettePort.findLatestActiveTable()).isPresent();
        BDDMockito.then(rouletteTableRepository).should(times(2)).findFirstByActiveTrueOrderByIdDesc();
    }

    @Test
    void commandActiveTrigger_ShouldCacheAndEvictWhenUpdated() {
        Command first = command(1L, "!호감도", CommandActionKey.FAVORITE_STATUS);
        Command second = command(2L, "!호감도", CommandActionKey.FAVORITE_STATUS);
        given(commandRepository.findByTriggerTokenAndActiveTrue("!호감도"))
                .willReturn(Optional.of(first))
                .willReturn(Optional.of(second));
        given(commandRepository.findById(1L)).willReturn(Optional.of(first));

        then(commandPort.findActiveByTrigger("!호감도").orElseThrow().id()).isEqualTo(1L);
        then(commandPort.findActiveByTrigger("!호감도").orElseThrow().id()).isEqualTo(1L);
        BDDMockito.then(commandRepository).should(times(1)).findByTriggerTokenAndActiveTrue("!호감도");

        commandPort.update(new CommandPort.UpdateData(1L, "!호감도", null, null, null, true, "USER", 30, "admin"));

        then(commandPort.findActiveByTrigger("!호감도").orElseThrow().id()).isEqualTo(2L);
        BDDMockito.then(commandRepository).should(times(2)).findByTriggerTokenAndActiveTrue("!호감도");
    }

    @Test
    void commandActiveActionKey_ShouldCacheAndEvictWhenCreated() {
        Command first = command(1L, "!룰렛", CommandActionKey.ROULETTE_DONATION);
        Command second = command(2L, "!룰렛", CommandActionKey.ROULETTE_DONATION);
        given(commandRepository.findByActionKeyAndActiveTrue(CommandActionKey.ROULETTE_DONATION))
                .willReturn(Optional.of(first))
                .willReturn(Optional.of(second));
        given(commandRepository.save(any(Command.class))).willReturn(second);

        then(commandPort.findActiveByActionKey(CommandActionKey.ROULETTE_DONATION).orElseThrow().id()).isEqualTo(1L);
        then(commandPort.findActiveByActionKey(CommandActionKey.ROULETTE_DONATION).orElseThrow().id()).isEqualTo(1L);
        BDDMockito.then(commandRepository)
                .should(times(1))
                .findByActionKeyAndActiveTrue(CommandActionKey.ROULETTE_DONATION);

        commandPort.create(new CommandPort.CreateData(
                CommandType.TRIGGER,
                "!새룰렛",
                CommandActionKey.ROULETTE_DONATION,
                null,
                null,
                null,
                true,
                "USER",
                0,
                "admin",
                "admin"
        ));

        then(commandPort.findActiveByActionKey(CommandActionKey.ROULETTE_DONATION).orElseThrow().id()).isEqualTo(2L);
        BDDMockito.then(commandRepository)
                .should(times(2))
                .findByActionKeyAndActiveTrue(CommandActionKey.ROULETTE_DONATION);
    }

    @Test
    void commandActiveTrigger_ShouldNotCacheMissingCommand() {
        given(commandRepository.findByTriggerTokenAndActiveTrue("!없음")).willReturn(Optional.empty());

        then(commandPort.findActiveByTrigger("!없음")).isEmpty();
        then(commandPort.findActiveByTrigger("!없음")).isEmpty();
        BDDMockito.then(commandRepository).should(times(2)).findByTriggerTokenAndActiveTrue("!없음");
    }

    private FavoriteAdjustment adjustment(Long id, Integer amount, String label) {
        return FavoriteAdjustment.builder()
                .id(id)
                .amount(amount)
                .label(label)
                .build();
    }

    private UpboTemplate template(Long id, String label) {
        return UpboTemplate.builder()
                .id(id)
                .label(label)
                .description("설명")
                .active(true)
                .displayOrder(1)
                .exchangeFavoriteValue(100)
                .rewardType(RewardType.FAVORITE)
                .conversionMode(ConversionMode.AUTO)
                .build();
    }

    private RouletteTable table(Long id, boolean active) {
        return RouletteTable.builder()
                .id(id)
                .title("기본 룰렛")
                .command("!룰렛")
                .pricePerRound(1_000L)
                .active(active)
                .version(0)
                .highRoundThreshold(100)
                .build();
    }

    private RouletteItem item(Long id, RouletteTable table, String label) {
        return RouletteItem.builder()
                .id(id)
                .rouletteTable(table)
                .label(label)
                .probabilityBasisPoints(10_000)
                .losingItem(false)
                .rewardType(RewardType.FAVORITE)
                .conversionMode(ConversionMode.AUTO)
                .exchangeFavoriteValue(100)
                .active(true)
                .displayOrder(1)
                .build();
    }

    private Command command(Long id, String trigger, CommandActionKey actionKey) {
        return Command.builder()
                .id(id)
                .type(CommandType.TRIGGER)
                .triggerToken(trigger)
                .actionKey(actionKey)
                .active(true)
                .requiredRole("USER")
                .userCooldownSeconds(30)
                .createdBy("system")
                .updatedBy("system")
                .build();
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        CommandPort commandPort(
                CommandRepository commandRepository,
                CommandPersistenceMapper commandPersistenceMapper
        ) {
            return new CommandPersistenceAdapter(commandRepository, commandPersistenceMapper);
        }

        @Bean
        FavoriteAdjustmentPort favoriteAdjustmentPort(
                FavoriteAdjustmentRepository favoriteAdjustmentRepository,
                FavoriteAdjustmentPersistenceMapper favoriteAdjustmentPersistenceMapper
        ) {
            return new FavoriteAdjustmentPersistenceAdapter(
                    favoriteAdjustmentRepository,
                    favoriteAdjustmentPersistenceMapper
            );
        }

        @Bean
        UpboPort upboPort(
                UpboTemplateRepository upboTemplateRepository,
                UserUpboRepository userUpboRepository,
                UpboPersistenceMapper upboPersistenceMapper
        ) {
            return new UpboPersistenceAdapter(upboTemplateRepository, userUpboRepository, upboPersistenceMapper);
        }

        @Bean
        RoulettePort roulettePort(
                RouletteTableRepository rouletteTableRepository,
                RouletteItemRepository rouletteItemRepository,
                RouletteEventRepository rouletteEventRepository,
                RouletteRoundResultRepository rouletteRoundResultRepository,
                RoulettePersistenceMapper roulettePersistenceMapper
        ) {
            return new RoulettePersistenceAdapter(
                    rouletteTableRepository,
                    rouletteItemRepository,
                    rouletteEventRepository,
                    rouletteRoundResultRepository,
                    roulettePersistenceMapper
            );
        }

        @Bean
        CommandPersistenceMapper commandPersistenceMapper() {
            return Mappers.getMapper(CommandPersistenceMapper.class);
        }

        @Bean
        FavoriteAdjustmentPersistenceMapper favoriteAdjustmentPersistenceMapper() {
            return Mappers.getMapper(FavoriteAdjustmentPersistenceMapper.class);
        }

        @Bean
        UpboPersistenceMapper upboPersistenceMapper() {
            return Mappers.getMapper(UpboPersistenceMapper.class);
        }

        @Bean
        RoulettePersistenceMapper roulettePersistenceMapper() {
            return Mappers.getMapper(RoulettePersistenceMapper.class);
        }

        @Bean
        FavoriteAdjustmentRepository favoriteAdjustmentRepository() {
            return BDDMockito.mock(FavoriteAdjustmentRepository.class);
        }

        @Bean
        UpboTemplateRepository upboTemplateRepository() {
            return BDDMockito.mock(UpboTemplateRepository.class);
        }

        @Bean
        UserUpboRepository userUpboRepository() {
            return BDDMockito.mock(UserUpboRepository.class);
        }

        @Bean
        RouletteTableRepository rouletteTableRepository() {
            return BDDMockito.mock(RouletteTableRepository.class);
        }

        @Bean
        RouletteItemRepository rouletteItemRepository() {
            return BDDMockito.mock(RouletteItemRepository.class);
        }

        @Bean
        RouletteEventRepository rouletteEventRepository() {
            return BDDMockito.mock(RouletteEventRepository.class);
        }

        @Bean
        RouletteRoundResultRepository rouletteRoundResultRepository() {
            return BDDMockito.mock(RouletteRoundResultRepository.class);
        }

        @Bean
        CommandRepository commandRepository() {
            return BDDMockito.mock(CommandRepository.class);
        }

    }
}
