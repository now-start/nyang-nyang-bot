package org.nowstart.nyangnyangbot.application.service.upbo;

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
import org.nowstart.nyangnyangbot.application.port.in.favorite.AdjustFavoriteUseCase.AdjustFavoriteCommand;
import org.nowstart.nyangnyangbot.application.port.in.favorite.AdjustFavoriteUseCase.FavoriteLedgerResult;
import org.nowstart.nyangnyangbot.application.port.in.favorite.AdjustFavoriteUseCase;
import org.nowstart.nyangnyangbot.application.port.in.upbo.ManageUpboUseCase.UpboApplyCommand;
import org.nowstart.nyangnyangbot.application.port.in.upbo.ManageUpboUseCase.UpboTemplateCreateCommand;
import org.nowstart.nyangnyangbot.application.port.in.upbo.ManageUpboUseCase.UpboTemplateResult;
import org.nowstart.nyangnyangbot.application.port.in.upbo.ManageUpboUseCase.UserUpboResult;
import org.nowstart.nyangnyangbot.application.port.out.upbo.UpboPort.CreateUserUpboCommand;
import org.nowstart.nyangnyangbot.application.port.out.upbo.UpboPort;
import org.nowstart.nyangnyangbot.domain.favorite.FavoriteSourceType;
import org.nowstart.nyangnyangbot.application.port.out.upbo.UpboPort.TemplateResult;
import org.nowstart.nyangnyangbot.application.port.out.upbo.UpboPort.UserResult;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
import org.nowstart.nyangnyangbot.domain.type.UpboStatus;

@ExtendWith(MockitoExtension.class)
class ManageUpboServiceTest {

    @Mock
    private UpboPort upboPort;

    @Mock
    private AdjustFavoriteUseCase adjustFavoriteUseCase;

    @InjectMocks
    private ManageUpboService upboService;

    @Test
    void createTemplate_ShouldPersistValidatedTemplate() {
        TemplateResult saved = new TemplateResult(
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

        UpboTemplateResult result = upboService.createTemplate(new UpboTemplateCreateCommand(
                "호감도 +10",
                "자동 전환",
                3,
                10,
                RewardType.FAVORITE.name(),
                ConversionMode.AUTO.name()
        ));

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.label()).isEqualTo("호감도 +10");
        then(upboPort).should().createTemplate(
                "호감도 +10", "자동 전환", 3, 10, RewardType.FAVORITE, ConversionMode.AUTO
        );
    }

    @Test
    void applyUpbo_ShouldConvertAutoTemplateThroughFavoriteLedger() {
        TemplateResult template = new TemplateResult(
                1L, "호감도 +10", "", true, 0, 10, RewardType.FAVORITE, ConversionMode.AUTO
        );
        given(upboPort.findTemplateById(1L)).willReturn(Optional.of(template));
        given(adjustFavoriteUseCase.adjust(any(AdjustFavoriteCommand.class)))
                .willReturn(new FavoriteLedgerResult("user-1", 20, 10, 30, "룰렛 수동 반영", false, 99L));
        given(upboPort.createUserUpbo(any(CreateUserUpboCommand.class)))
                .willReturn(userUpbo(1L, UpboStatus.CONVERTED, 99L));

        UserUpboResult result = upboService.applyUpbo(new UpboApplyCommand(
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

        assertThat(result.status()).isEqualTo(UpboStatus.CONVERTED.name());
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

        UserUpboResult result = upboService.applyUpbo(new UpboApplyCommand(
                "user-1",
                "치즈냥",
                null,
                "칭찬 쿠폰",
                RewardType.COUPON.name(),
                ConversionMode.NONE.name(),
                null,
                "칭찬 쿠폰 지급",
                "관리자 확인"
        ), "admin-1");

        assertThat(result.status()).isEqualTo(UpboStatus.OWNED.name());
        assertThat(result.ledgerId()).isNull();
        then(adjustFavoriteUseCase).should(never()).adjust(any());
    }

    @Test
    void applyUpbo_ShouldRequirePrivateMemoForFreeInput() {
        assertThatThrownBy(() -> upboService.applyUpbo(new UpboApplyCommand(
                "user-1",
                "치즈냥",
                null,
                "자유 입력",
                RewardType.CUSTOM.name(),
                ConversionMode.NONE.name(),
                null,
                "공개 설명",
                ""
        ), "admin-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("privateMemo is required");
    }

    private UserResult userUpbo(Long id, UpboStatus status, Long ledgerId) {
        return new UserResult(
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
