package org.nowstart.nyangnyangbot.application.service.favorite;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;

import jakarta.validation.Validation;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.application.port.in.favorite.AdjustFavoriteUseCase;
import org.nowstart.nyangnyangbot.application.port.in.favorite.AdjustFavoriteUseCase.AdjustFavoriteCommand;
import org.nowstart.nyangnyangbot.application.port.in.favorite.AdjustFavoriteUseCase.FavoriteLedgerResult;
import org.nowstart.nyangnyangbot.application.port.in.favorite.ManageFavoriteAdjustmentUseCase.FavoriteAdjustmentApplyCommand;
import org.nowstart.nyangnyangbot.application.port.in.favorite.ManageFavoriteAdjustmentUseCase.FavoriteAdjustmentApplyResult;
import org.nowstart.nyangnyangbot.application.port.in.favorite.ManageFavoriteAdjustmentUseCase.FavoriteAdjustmentCreateCommand;
import org.nowstart.nyangnyangbot.application.port.in.favorite.ManageFavoriteAdjustmentUseCase.FavoriteAdjustmentOptionResult;
import org.nowstart.nyangnyangbot.application.port.out.favorite.FavoriteAdjustmentPort;
import org.nowstart.nyangnyangbot.application.port.out.favorite.FavoriteAdjustmentPort.OptionResult;
import org.nowstart.nyangnyangbot.application.validation.UseCaseValidator;
import org.nowstart.nyangnyangbot.domain.favorite.FavoriteSourceType;

@ExtendWith(MockitoExtension.class)
class FavoriteAdjustmentServiceTest {

    @Mock
    private FavoriteAdjustmentPort favoriteAdjustmentPort;

    @Mock
    private AdjustFavoriteUseCase adjustFavoriteUseCase;

    @Test
    void getAdjustments_ShouldSortOptionsByAmount() {
        // 준비
        FavoriteAdjustmentService service = service();
        given(favoriteAdjustmentPort.findAll()).willReturn(List.of(
                new OptionResult(2L, 50, "큰 보정"),
                new OptionResult(1L, -10, "차감")
        ));

        // 실행
        List<FavoriteAdjustmentOptionResult> result = service.getAdjustments();

        // 검증
        then(result).extracting(FavoriteAdjustmentOptionResult::amount)
                .containsExactly(-10, 50);
    }

    @Test
    void createAdjustment_ShouldTrimLabelAndReturnSavedOption() {
        // 준비
        FavoriteAdjustmentService service = service();
        given(favoriteAdjustmentPort.save(30, "보너스")).willReturn(new OptionResult(3L, 30, "보너스"));

        // 실행
        FavoriteAdjustmentOptionResult result = service.createAdjustment(new FavoriteAdjustmentCreateCommand(30, " 보너스 "));

        // 검증
        then(result.id()).isEqualTo(3L);
        then(result.amount()).isEqualTo(30);
        then(result.label()).isEqualTo("보너스");
        BDDMockito.then(favoriteAdjustmentPort).should().save(30, "보너스");
    }

    @Test
    void createAdjustment_ShouldRejectInvalidCommand() {
        // 준비
        FavoriteAdjustmentService service = service();

        // 실행 및 검증
        thenThrownBy(() -> service.createAdjustment(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("request is required");
        thenThrownBy(() -> service.createAdjustment(new FavoriteAdjustmentCreateCommand(null, "보너스")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("amount is required");
        thenThrownBy(() -> service.createAdjustment(new FavoriteAdjustmentCreateCommand(10, " ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("label is required");
    }

    @Test
    void applyAdjustments_ShouldApplySelectedOptionsAndManualAmount() {
        // 준비
        FavoriteAdjustmentService service = service();
        given(favoriteAdjustmentPort.findAll()).willReturn(List.of(
                new OptionResult(1L, 10, "출석 보너스"),
                new OptionResult(2L, -3, "벌점")
        ));
        given(adjustFavoriteUseCase.adjust(any(AdjustFavoriteCommand.class)))
                .willReturn(new FavoriteLedgerResult("user-1", 5, 12, 17, "history", false, 99L));

        // 실행
        FavoriteAdjustmentApplyResult result = service.applyAdjustments(new FavoriteAdjustmentApplyCommand(
                "user-1",
                List.of(1L, 2L),
                5,
                "수동 보정"
        ));

        // 검증
        then(result.userId()).isEqualTo("user-1");
        then(result.delta()).isEqualTo(12);
        then(result.history()).isEqualTo("업보 적용: 출석 보너스(+10), 벌점(-3), 수동 보정(+5)");
        BDDMockito.then(adjustFavoriteUseCase).should().adjust(argThat(command ->
                command.userId().equals("user-1")
                        && command.delta() == 12
                        && command.sourceType() == FavoriteSourceType.ADMIN_ADJUSTMENT
                        && command.displayCategory().equals("ADMIN")
                        && command.publicDescription().equals(result.history())
                        && command.allowNegativeBalance()
                        && !command.createIfMissing()
        ));
    }

    @Test
    void applyAdjustments_ShouldApplyManualAmountOnlyWithDefaultHistoryLabel() {
        // 준비
        FavoriteAdjustmentService service = service();
        given(adjustFavoriteUseCase.adjust(any(AdjustFavoriteCommand.class)))
                .willReturn(new FavoriteLedgerResult("user-1", 20, -4, 16, "history", false, 100L));

        // 실행
        FavoriteAdjustmentApplyResult result = service.applyAdjustments(new FavoriteAdjustmentApplyCommand(
                "user-1",
                null,
                -4,
                " "
        ));

        // 검증
        then(result.delta()).isEqualTo(-4);
        then(result.history()).isEqualTo("업보 적용: 수동 입력(-4)");
        BDDMockito.then(adjustFavoriteUseCase).should().adjust(argThat(command ->
                command.delta() == -4
                        && "업보 적용: 수동 입력(-4)".equals(command.publicDescription())
        ));
    }

    @Test
    void applyAdjustments_ShouldApplySelectedOptionsWithoutManualAmount() {
        // 준비
        FavoriteAdjustmentService service = service();
        given(favoriteAdjustmentPort.findAll()).willReturn(List.of(
                new OptionResult(1L, 10, "출석 보너스")
        ));
        given(adjustFavoriteUseCase.adjust(any(AdjustFavoriteCommand.class)))
                .willReturn(new FavoriteLedgerResult("user-1", 5, 10, 15, "history", false, 101L));

        // 실행
        FavoriteAdjustmentApplyResult result = service.applyAdjustments(new FavoriteAdjustmentApplyCommand(
                "user-1",
                List.of(1L),
                null,
                null
        ));

        // 검증
        then(result.delta()).isEqualTo(10);
        then(result.history()).isEqualTo("업보 적용: 출석 보너스(+10)");
    }

    @Test
    void applyAdjustments_ShouldRejectMissingAdjustmentIds() {
        // 준비
        FavoriteAdjustmentService service = service();
        given(favoriteAdjustmentPort.findAll()).willReturn(List.of(
                new OptionResult(1L, 10, "출석 보너스")
        ));

        // 실행 및 검증
        thenThrownBy(() -> service.applyAdjustments(new FavoriteAdjustmentApplyCommand(
                "user-1",
                List.of(1L, 2L),
                null,
                null
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Missing adjustments: [2]");
        BDDMockito.then(adjustFavoriteUseCase).shouldHaveNoInteractions();
    }

    @Test
    void applyAdjustments_ShouldRejectMissingTargetAndDelta() {
        // 준비
        FavoriteAdjustmentService service = service();

        // 실행 및 검증
        thenThrownBy(() -> service.applyAdjustments(new FavoriteAdjustmentApplyCommand(" ", List.of(), null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("userId is required");
        thenThrownBy(() -> service.applyAdjustments(new FavoriteAdjustmentApplyCommand("user-1", List.of(), 0, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("adjustmentIds or manualAmount is required");
    }

    private FavoriteAdjustmentService service() {
        return new FavoriteAdjustmentService(favoriteAdjustmentPort, adjustFavoriteUseCase, validator());
    }

    private UseCaseValidator validator() {
        return new UseCaseValidator(Validation.buildDefaultValidatorFactory().getValidator());
    }
}
