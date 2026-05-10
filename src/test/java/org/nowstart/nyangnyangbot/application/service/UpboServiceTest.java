package org.nowstart.nyangnyangbot.application.service;

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
import org.nowstart.nyangnyangbot.domain.model.UpboTemplate;
import org.nowstart.nyangnyangbot.domain.model.UserUpbo;
import org.nowstart.nyangnyangbot.application.gateway.out.upbo.CreateUserUpboCommand;
import org.nowstart.nyangnyangbot.application.gateway.out.upbo.UpboPort;
import org.nowstart.nyangnyangbot.application.upbo.dto.UpboApplyDto;
import org.nowstart.nyangnyangbot.application.upbo.dto.UpboTemplateDto;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
import org.nowstart.nyangnyangbot.domain.type.UpboStatus;
import org.nowstart.nyangnyangbot.domain.favorite.FavoriteSourceType;

@ExtendWith(MockitoExtension.class)
class UpboServiceTest {

    @Mock
    private UpboPort upboPort;

    @Mock
    private AdjustFavoriteUseCase adjustFavoriteUseCase;

    @InjectMocks
    private UpboService upboService;

    @Test
    void createTemplate_ShouldPersistValidatedTemplate() {
        UpboTemplate saved = new UpboTemplate(
                1L,
                "호감도 +10",
                "자동 전환",
                true,
                3,
                10,
                RewardType.FAVORITE,
                ConversionMode.AUTO
        );
        given(upboPort.createTemplate(
                "호감도 +10", "자동 전환", 3, 10, RewardType.FAVORITE, ConversionMode.AUTO
        )).willReturn(saved);

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
        then(upboPort).should().createTemplate(
                "호감도 +10", "자동 전환", 3, 10, RewardType.FAVORITE, ConversionMode.AUTO
        );
    }

    @Test
    void applyUpbo_ShouldConvertAutoTemplateThroughFavoriteLedger() {
        UpboTemplate template = new UpboTemplate(
                1L, "호감도 +10", "", true, 0, 10, RewardType.FAVORITE, ConversionMode.AUTO
        );
        given(upboPort.findTemplateById(1L)).willReturn(Optional.of(template));
        given(adjustFavoriteUseCase.adjust(any(AdjustFavoriteCommand.class)))
                .willReturn(new FavoriteLedgerResult("user-1", 20, 10, 30, "룰렛 수동 반영", false, 99L));
        given(upboPort.createUserUpbo(any(CreateUserUpboCommand.class)))
                .willReturn(userUpbo(1L, UpboStatus.CONVERTED, 99L));

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
        then(upboPort).should().createUserUpbo(argThat(command ->
                command.status() == UpboStatus.CONVERTED
                        && Long.valueOf(99L).equals(command.ledgerId())
                        && "관리자 확인".equals(command.privateMemo())
        ));
    }

    @Test
    void applyUpbo_ShouldKeepNoneConversionAsOwnedWithoutFavoriteLedger() {
        given(upboPort.createUserUpbo(any(CreateUserUpboCommand.class)))
                .willReturn(userUpbo(1L, UpboStatus.OWNED, null));

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

    private UserUpbo userUpbo(Long id, UpboStatus status, Long ledgerId) {
        return new UserUpbo(
                id,
                "user-1",
                1L,
                "치즈냥",
                "호감도 +10",
                status,
                10,
                RewardType.FAVORITE,
                ConversionMode.AUTO,
                FavoriteSourceType.UPBO_MANUAL,
                ledgerId,
                "룰렛 수동 반영",
                "관리자 확인",
                "admin-1",
                null
        );
    }
}
