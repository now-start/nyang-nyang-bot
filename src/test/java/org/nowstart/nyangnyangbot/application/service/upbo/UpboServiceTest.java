package org.nowstart.nyangnyangbot.application.service.upbo;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import org.mockito.BDDMockito;
import static org.mockito.Mockito.never;

import jakarta.validation.Validation;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import org.nowstart.nyangnyangbot.application.validation.UseCaseValidator;
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

    private ManageUpboService upboService;

    @BeforeEach
    void setUp() {
        upboService = new ManageUpboService(upboPort, adjustFavoriteUseCase, validator());
    }

    @Test
    void getActiveTemplates_ShouldMapNullableTemplateFields() {
        // 준비
        TemplateResult template = new TemplateResult(
                2L,
                "커스텀 업보",
                null,
                true,
                null,
                null,
                null,
                null
        );
        given(upboPort.findActiveTemplates()).willReturn(List.of(template));

        // 실행
        List<UpboTemplateResult> result = upboService.getActiveTemplates();

        // 검증
        then(result).hasSize(1);
        then(result.getFirst().id()).isEqualTo(2L);
        then(result.getFirst().rewardType()).isNull();
        then(result.getFirst().conversionMode()).isNull();
        BDDMockito.then(upboPort).should().findActiveTemplates();
    }

    @Test
    void createTemplate_ShouldPersistValidatedTemplate() {
        // 준비
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

        // 실행
        UpboTemplateResult result = upboService.createTemplate(new UpboTemplateCreateCommand(
                "호감도 +10",
                "자동 전환",
                3,
                10,
                RewardType.FAVORITE.name(),
                ConversionMode.AUTO.name()
        ));

        // 검증
        then(result.id()).isEqualTo(1L);
        then(result.label()).isEqualTo("호감도 +10");
        BDDMockito.then(upboPort).should().createTemplate(
                "호감도 +10", "자동 전환", 3, 10, RewardType.FAVORITE, ConversionMode.AUTO
        );
    }

    @Test
    void createTemplate_ShouldUseDefaultDisplayOrderAndEmptyDescription() {
        // 준비
        TemplateResult saved = new TemplateResult(
                2L,
                "칭찬 쿠폰",
                "",
                true,
                0,
                null,
                RewardType.COUPON,
                ConversionMode.NONE
        );
        given(upboPort.createTemplate(
                "칭찬 쿠폰", "", 0, null, RewardType.COUPON, ConversionMode.NONE
        )).willReturn(saved);

        // 실행
        UpboTemplateResult result = upboService.createTemplate(new UpboTemplateCreateCommand(
                " 칭찬 쿠폰 ",
                null,
                null,
                null,
                " COUPON ",
                " NONE "
        ));

        // 검증
        then(result.displayOrder()).isZero();
        then(result.description()).isEmpty();
        BDDMockito.then(upboPort).should().createTemplate(
                "칭찬 쿠폰", "", 0, null, RewardType.COUPON, ConversionMode.NONE
        );
    }

    @Test
    void createTemplate_ShouldRejectInvalidTemplateCommand() {
        // 실행 및 검증
        thenThrownBy(() -> upboService.createTemplate(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("request is required");
        thenThrownBy(() -> upboService.createTemplate(new UpboTemplateCreateCommand(
                "업보",
                null,
                null,
                null,
                " ",
                ConversionMode.NONE.name()
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("rewardType is required");
        thenThrownBy(() -> upboService.createTemplate(new UpboTemplateCreateCommand(
                "업보",
                null,
                null,
                null,
                RewardType.CUSTOM.name(),
                " "
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("conversionMode is required");
        thenThrownBy(() -> upboService.createTemplate(new UpboTemplateCreateCommand(
                "업보",
                null,
                null,
                0,
                RewardType.FAVORITE.name(),
                ConversionMode.AUTO.name()
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("exchangeFavoriteValue is required for AUTO conversion");
    }

    @Test
    void applyUpbo_ShouldConvertAutoTemplateThroughFavoriteLedger() {
        // 준비
        TemplateResult template = new TemplateResult(
                1L, "호감도 +10", "", true, 0, 10, RewardType.FAVORITE, ConversionMode.AUTO
        );
        given(upboPort.findTemplateById(1L)).willReturn(Optional.of(template));
        given(adjustFavoriteUseCase.adjust(any(AdjustFavoriteCommand.class)))
                .willReturn(new FavoriteLedgerResult("user-1", 20, 10, 30, "룰렛 수동 반영", false, 99L));
        given(upboPort.createUserUpbo(any(CreateUserUpboCommand.class)))
                .willReturn(userUpbo(1L, UpboStatus.CONVERTED, 99L));

        // 실행
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

        // 검증
        then(result.status()).isEqualTo(UpboStatus.CONVERTED.name());
        then(result.ledgerId()).isEqualTo(99L);
        BDDMockito.then(adjustFavoriteUseCase).should().adjust(argThat(command ->
                "user-1".equals(command.userId())
                        && command.delta() == 10
                        && command.sourceType() == FavoriteSourceType.UPBO_MANUAL
                        && "룰렛 수동 반영".equals(command.publicDescription())
                        && "관리자 확인".equals(command.privateMemo())
                        && "admin-1".equals(command.actorId())
        ));
        BDDMockito.then(upboPort).should().createUserUpbo(argThat(command ->
                command.status() == UpboStatus.CONVERTED
                        && Long.valueOf(99L).equals(command.ledgerId())
                        && "관리자 확인".equals(command.privateMemo())
        ));
    }

    @Test
    void applyUpbo_ShouldCreateAutoFreeInputWithTrimmedValues() {
        // 준비
        given(adjustFavoriteUseCase.adjust(any(AdjustFavoriteCommand.class)))
                .willReturn(new FavoriteLedgerResult("user-1", 0, -5, -5, "차감", false, 100L));
        given(upboPort.createUserUpbo(any(CreateUserUpboCommand.class)))
                .willReturn(userUpbo(2L, UpboStatus.CONVERTED, 100L));

        // 실행
        UserUpboResult result = upboService.applyUpbo(new UpboApplyCommand(
                "user-1",
                null,
                null,
                " 호감도 차감 ",
                " FAVORITE ",
                " AUTO ",
                -5,
                " 공개 차감 ",
                " 내부 차감 "
        ), "admin-1");

        // 검증
        then(result.status()).isEqualTo(UpboStatus.CONVERTED.name());
        BDDMockito.then(adjustFavoriteUseCase).should().adjust(argThat(command ->
                command.delta() == -5
                        && "공개 차감".equals(command.publicDescription())
                        && "내부 차감".equals(command.privateMemo())
                        && command.allowNegativeBalance()
                        && command.createIfMissing()
        ));
        BDDMockito.then(upboPort).should().createUserUpbo(argThat(command ->
                command.upboTemplateId() == null
                        && command.nickNameSnapshot().isEmpty()
                        && "호감도 차감".equals(command.label())
                        && command.status() == UpboStatus.CONVERTED
                        && command.exchangeFavoriteValue() == -5
                        && Long.valueOf(100L).equals(command.ledgerId())
        ));
    }

    @Test
    void applyUpbo_ShouldKeepNoneConversionAsOwnedWithoutFavoriteLedger() {
        // 준비
        given(upboPort.createUserUpbo(any(CreateUserUpboCommand.class)))
                .willReturn(userUpbo(1L, UpboStatus.OWNED, null));

        // 실행
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

        // 검증
        then(result.status()).isEqualTo(UpboStatus.OWNED.name());
        then(result.ledgerId()).isNull();
        BDDMockito.then(adjustFavoriteUseCase).should(never()).adjust(any());
    }

    @Test
    void applyUpbo_ShouldRejectNullRequestAndMissingTemplate() {
        // 준비
        given(upboPort.findTemplateById(404L)).willReturn(Optional.empty());

        // 실행 및 검증
        thenThrownBy(() -> upboService.applyUpbo(null, "admin-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("request is required");
        thenThrownBy(() -> upboService.applyUpbo(new UpboApplyCommand(
                "user-1",
                "치즈냥",
                404L,
                null,
                null,
                null,
                null,
                "공개 설명",
                "관리자 확인"
        ), "admin-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("upbo template not found");
    }

    @Test
    void applyUpbo_ShouldValidateFreeInputFields() {
        // 실행 및 검증
        thenThrownBy(() -> upboService.applyUpbo(new UpboApplyCommand(
                " ",
                "치즈냥",
                null,
                "자유 입력",
                RewardType.CUSTOM.name(),
                ConversionMode.NONE.name(),
                null,
                "공개 설명",
                "관리자 확인"
        ), "admin-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("userId is required");
        thenThrownBy(() -> upboService.applyUpbo(new UpboApplyCommand(
                "user-1",
                "치즈냥",
                null,
                " ",
                RewardType.CUSTOM.name(),
                ConversionMode.NONE.name(),
                null,
                "공개 설명",
                "관리자 확인"
        ), "admin-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("label is required");
        thenThrownBy(() -> upboService.applyUpbo(new UpboApplyCommand(
                "user-1",
                "치즈냥",
                null,
                "자유 입력",
                " ",
                ConversionMode.NONE.name(),
                null,
                "공개 설명",
                "관리자 확인"
        ), "admin-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("rewardType is required");
        thenThrownBy(() -> upboService.applyUpbo(new UpboApplyCommand(
                "user-1",
                "치즈냥",
                null,
                "자유 입력",
                RewardType.CUSTOM.name(),
                " ",
                null,
                "공개 설명",
                "관리자 확인"
        ), "admin-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("conversionMode is required");
        thenThrownBy(() -> upboService.applyUpbo(new UpboApplyCommand(
                "user-1",
                "치즈냥",
                null,
                "자유 입력",
                RewardType.FAVORITE.name(),
                ConversionMode.AUTO.name(),
                null,
                "공개 설명",
                "관리자 확인"
        ), "admin-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("exchangeFavoriteValue is required for AUTO conversion");
        thenThrownBy(() -> upboService.applyUpbo(new UpboApplyCommand(
                "user-1",
                "치즈냥",
                null,
                "자유 입력",
                RewardType.CUSTOM.name(),
                ConversionMode.NONE.name(),
                null,
                " ",
                "관리자 확인"
        ), "admin-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("publicDescription is required");
    }

    @Test
    void applyUpbo_ShouldRequirePrivateMemoForFreeInput() {
        // 실행 및 검증
        thenThrownBy(() -> upboService.applyUpbo(new UpboApplyCommand(
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

    private UseCaseValidator validator() {
        return new UseCaseValidator(Validation.buildDefaultValidatorFactory().getValidator());
    }
}
