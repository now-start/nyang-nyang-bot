package org.nowstart.nyangnyangbot.adapter.out.persistence.upbo;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.nowstart.nyangnyangbot.support.OutboundContractTestSupport.outboundContractValidator;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.adapter.out.persistence.upbo.entity.UpboTemplate;
import org.nowstart.nyangnyangbot.adapter.out.persistence.upbo.entity.UserUpbo;
import org.nowstart.nyangnyangbot.adapter.out.persistence.upbo.repository.UpboTemplateRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.upbo.repository.UserUpboRepository;
import org.nowstart.nyangnyangbot.application.port.out.upbo.UpboPort.CreateUserUpboCommand;
import org.nowstart.nyangnyangbot.application.port.out.upbo.UpboPort.TemplateResult;
import org.nowstart.nyangnyangbot.application.port.out.upbo.UpboPort.UserResult;
import org.nowstart.nyangnyangbot.domain.favorite.FavoriteSourceType;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
import org.nowstart.nyangnyangbot.domain.type.UpboStatus;

@ExtendWith(MockitoExtension.class)
class UpboPersistenceAdapterTest {

    @Mock
    private UpboTemplateRepository upboTemplateRepository;

    @Mock
    private UserUpboRepository userUpboRepository;

    @Test
    void templateQueries_ShouldMapTemplateEntities() {
        // 준비
        UpboPersistenceAdapter adapter = adapter();
        UpboTemplate template = template(1L);
        given(upboTemplateRepository.findByActiveTrueOrderByDisplayOrderAscIdAsc()).willReturn(List.of(template));
        given(upboTemplateRepository.findById(1L)).willReturn(Optional.of(template));
        given(upboTemplateRepository.save(any(UpboTemplate.class))).willReturn(template);

        // 실행
        List<TemplateResult> activeTemplates = adapter.findActiveTemplates();
        Optional<TemplateResult> found = adapter.findTemplateById(1L);
        TemplateResult created = adapter.createTemplate(
                "호감도 +100",
                "설명",
                1,
                100,
                RewardType.FAVORITE,
                ConversionMode.AUTO
        );

        // 검증
        then(activeTemplates).hasSize(1);
        then(found).isPresent();
        then(created.id()).isEqualTo(1L);
        then(created.rewardType()).isEqualTo(RewardType.FAVORITE);
    }

    @Test
    void createUserUpbo_ShouldUseTemplateReferenceWhenTemplateIdExists() {
        // 준비
        UpboPersistenceAdapter adapter = adapter();
        UpboTemplate template = template(1L);
        UserUpbo userUpbo = userUpbo(2L, template);
        given(upboTemplateRepository.getReferenceById(1L)).willReturn(template);
        given(userUpboRepository.save(any(UserUpbo.class))).willReturn(userUpbo);

        // 실행
        UserResult result = adapter.createUserUpbo(new CreateUserUpboCommand(
                "user-1",
                1L,
                "치즈냥",
                "호감도 +100",
                UpboStatus.CONVERTED,
                100,
                RewardType.FAVORITE,
                ConversionMode.AUTO,
                FavoriteSourceType.UPBO_MANUAL,
                10L,
                "공개",
                "내부",
                "admin-1"
        ));

        // 검증
        then(result.id()).isEqualTo(2L);
        then(result.upboTemplateId()).isEqualTo(1L);
        then(result.status()).isEqualTo(UpboStatus.CONVERTED);
        BDDMockito.then(upboTemplateRepository).should().getReferenceById(1L);
    }

    @Test
    void userQueries_ShouldMapUserUpbosWithoutTemplate() {
        // 준비
        UpboPersistenceAdapter adapter = adapter();
        UserUpbo userUpbo = userUpbo(2L, null);
        given(userUpboRepository.save(any(UserUpbo.class))).willReturn(userUpbo);
        given(userUpboRepository.findByUserIdOrderByCreateDateDesc("user-1")).willReturn(List.of(userUpbo));
        given(userUpboRepository.findByUserIdAndStatusOrderByCreateDateDesc("user-1", UpboStatus.OWNED))
                .willReturn(List.of(userUpbo));

        // 실행
        UserResult created = adapter.createUserUpbo(new CreateUserUpboCommand(
                "user-1",
                null,
                "치즈냥",
                "직접 입력",
                UpboStatus.OWNED,
                null,
                RewardType.CUSTOM,
                ConversionMode.NONE,
                FavoriteSourceType.UPBO_MANUAL,
                null,
                "공개",
                "내부",
                "admin-1"
        ));
        List<UserResult> all = adapter.findUserUpbos("user-1");
        List<UserResult> owned = adapter.findUserUpbosByStatus("user-1", UpboStatus.OWNED);

        // 검증
        then(created.upboTemplateId()).isNull();
        then(created.label()).isEqualTo("직접 입력");
        then(all).hasSize(1);
        then(owned).hasSize(1);
    }

    private UpboPersistenceAdapter adapter() {
        return new UpboPersistenceAdapter(
                upboTemplateRepository,
                userUpboRepository,
                outboundContractValidator()
        );
    }

    private UpboTemplate template(Long id) {
        return UpboTemplate.builder()
                .id(id)
                .label("호감도 +100")
                .description("설명")
                .active(true)
                .displayOrder(1)
                .exchangeFavoriteValue(100)
                .rewardType(RewardType.FAVORITE)
                .conversionMode(ConversionMode.AUTO)
                .build();
    }

    private UserUpbo userUpbo(Long id, UpboTemplate template) {
        return UserUpbo.builder()
                .id(id)
                .userId("user-1")
                .upboTemplate(template)
                .nickNameSnapshot("치즈냥")
                .label(template == null ? "직접 입력" : template.getLabel())
                .status(template == null ? UpboStatus.OWNED : UpboStatus.CONVERTED)
                .exchangeFavoriteValue(template == null ? null : template.getExchangeFavoriteValue())
                .rewardType(template == null ? RewardType.CUSTOM : template.getRewardType())
                .conversionMode(template == null ? ConversionMode.NONE : template.getConversionMode())
                .sourceType(FavoriteSourceType.UPBO_MANUAL)
                .ledgerId(template == null ? null : 10L)
                .publicDescription("공개")
                .privateMemo("내부")
                .actorId("admin-1")
                .build();
    }
}
