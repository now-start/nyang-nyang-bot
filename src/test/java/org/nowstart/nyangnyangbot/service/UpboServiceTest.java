package org.nowstart.nyangnyangbot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.application.favorite.AdjustFavoriteCommand;
import org.nowstart.nyangnyangbot.application.favorite.AdjustFavoriteUseCase;
import org.nowstart.nyangnyangbot.application.favorite.FavoriteLedgerResult;
import org.nowstart.nyangnyangbot.data.dto.upbo.UpboApplyDto;
import org.nowstart.nyangnyangbot.data.dto.upbo.UpboTemplateDto;
import org.nowstart.nyangnyangbot.data.entity.UpboTemplateEntity;
import org.nowstart.nyangnyangbot.data.entity.UserUpboEntity;
import org.nowstart.nyangnyangbot.data.type.ConversionMode;
import org.nowstart.nyangnyangbot.data.type.RewardType;
import org.nowstart.nyangnyangbot.data.type.UpboStatus;
import org.nowstart.nyangnyangbot.domain.favorite.FavoriteSourceType;
import org.nowstart.nyangnyangbot.repository.UpboTemplateRepository;
import org.nowstart.nyangnyangbot.repository.UserUpboRepository;

@ExtendWith(MockitoExtension.class)
class UpboServiceTest {

    @Mock
    private UpboTemplateRepository upboTemplateRepository;

    @Mock
    private UserUpboRepository userUpboRepository;

    @Mock
    private AdjustFavoriteUseCase adjustFavoriteUseCase;

    @InjectMocks
    private UpboService upboService;

    @Test
    void createTemplate_ShouldPersistValidatedTemplate() {
        UpboTemplateEntity saved = UpboTemplateEntity.builder()
                .id(1L)
                .label("호감도 +10")
                .description("자동 전환")
                .active(true)
                .displayOrder(3)
                .exchangeFavoriteValue(10)
                .rewardType(RewardType.FAVORITE)
                .conversionMode(ConversionMode.AUTO)
                .build();
        given(upboTemplateRepository.save(any(UpboTemplateEntity.class))).willReturn(saved);

        UpboTemplateDto.Response result = upboService.createTemplate(new UpboTemplateDto.CreateRequest(
                "호감도 +10",
                "자동 전환",
                3,
                10,
                RewardType.FAVORITE,
                ConversionMode.AUTO
        ));

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.label()).isEqualTo("호감도 +10");
        then(upboTemplateRepository).should().save(argThat(entity ->
                entity.isActive()
                        && entity.getExchangeFavoriteValue().equals(10)
                        && entity.getConversionMode() == ConversionMode.AUTO
        ));
    }

    @Test
    void applyUpbo_ShouldConvertAutoTemplateThroughFavoriteLedger() {
        UpboTemplateEntity template = UpboTemplateEntity.builder()
                .id(1L)
                .label("호감도 +10")
                .active(true)
                .exchangeFavoriteValue(10)
                .rewardType(RewardType.FAVORITE)
                .conversionMode(ConversionMode.AUTO)
                .build();
        given(upboTemplateRepository.findById(1L)).willReturn(Optional.of(template));
        given(adjustFavoriteUseCase.adjust(any(AdjustFavoriteCommand.class)))
                .willReturn(new FavoriteLedgerResult("user-1", 20, 10, 30, "룰렛 수동 반영", false, 99L));
        given(userUpboRepository.save(any(UserUpboEntity.class))).willAnswer(invocation -> invocation.getArgument(0));

        UpboApplyDto.Response result = upboService.applyUpbo(new UpboApplyDto.Request(
                "user-1",
                "치즈냥",
                1L,
                null,
                null,
                null,
                null,
                "룰렛 수동 반영",
                "관리자 확인"
        ), "admin-1");

        assertThat(result.status()).isEqualTo(UpboStatus.CONVERTED);
        assertThat(result.ledgerId()).isEqualTo(99L);
        then(adjustFavoriteUseCase).should().adjust(argThat(command ->
                "user-1".equals(command.userId())
                        && command.delta() == 10
                        && command.sourceType() == FavoriteSourceType.UPBO_MANUAL
                        && "룰렛 수동 반영".equals(command.publicDescription())
                        && "관리자 확인".equals(command.privateMemo())
                        && "admin-1".equals(command.actorId())
        ));
        then(userUpboRepository).should().save(argThat(entity ->
                entity.getStatus() == UpboStatus.CONVERTED
                        && Long.valueOf(99L).equals(entity.getLedgerId())
                        && "관리자 확인".equals(entity.getPrivateMemo())
        ));
    }

    @Test
    void applyUpbo_ShouldKeepNoneConversionAsOwnedWithoutFavoriteLedger() {
        given(userUpboRepository.save(any(UserUpboEntity.class))).willAnswer(invocation -> invocation.getArgument(0));

        UpboApplyDto.Response result = upboService.applyUpbo(new UpboApplyDto.Request(
                "user-1",
                "치즈냥",
                null,
                "칭찬 쿠폰",
                RewardType.COUPON,
                ConversionMode.NONE,
                null,
                "칭찬 쿠폰 지급",
                "관리자 확인"
        ), "admin-1");

        assertThat(result.status()).isEqualTo(UpboStatus.OWNED);
        assertThat(result.ledgerId()).isNull();
        then(adjustFavoriteUseCase).should(never()).adjust(any());
    }

    @Test
    void applyUpbo_ShouldRequirePrivateMemoForFreeInput() {
        assertThatThrownBy(() -> upboService.applyUpbo(new UpboApplyDto.Request(
                "user-1",
                "치즈냥",
                null,
                "자유 입력",
                RewardType.CUSTOM,
                ConversionMode.NONE,
                null,
                "공개 설명",
                ""
        ), "admin-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("privateMemo is required");
    }
}
