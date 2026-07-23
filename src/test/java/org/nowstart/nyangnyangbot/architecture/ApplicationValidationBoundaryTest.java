package org.nowstart.nyangnyangbot.architecture;

import static org.assertj.core.api.BDDAssertions.then;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.nowstart.nyangnyangbot.application.port.in.command.ManageCommandUseCase;
import org.nowstart.nyangnyangbot.application.port.in.point.AdjustPointUseCase;
import org.nowstart.nyangnyangbot.application.port.in.point.GrantPointUseCase;
import org.nowstart.nyangnyangbot.application.port.in.point.ManagePointAdjustmentPresetUseCase;
import org.nowstart.nyangnyangbot.application.port.in.point.ReconcilePointBalanceUseCase;
import org.nowstart.nyangnyangbot.application.port.in.presence.ManagePresenceRewardUseCase;
import org.nowstart.nyangnyangbot.application.port.in.roulette.ManageRouletteUseCase;
import org.nowstart.nyangnyangbot.application.service.command.CommandService;
import org.nowstart.nyangnyangbot.application.service.point.PointAdjustmentPresetService;
import org.nowstart.nyangnyangbot.application.service.point.PointLedgerService;
import org.nowstart.nyangnyangbot.application.service.presence.PresenceRewardService;
import org.nowstart.nyangnyangbot.application.service.roulette.ManageRouletteService;
import org.springframework.validation.annotation.Validated;

class ApplicationValidationBoundaryTest {

    @Test
    void commandHandlingServices_ShouldEnableMethodValidation() {
        List<Class<?>> services = List.of(
                PresenceRewardService.class,
                CommandService.class,
                PointAdjustmentPresetService.class,
                PointLedgerService.class,
                ManageRouletteService.class
        );

        then(services).allSatisfy(service ->
                then(service.isAnnotationPresent(Validated.class))
                        .as(service.getSimpleName())
                        .isTrue());
    }

    @Test
    void commandParameters_ShouldDeclareCascadedValidation() throws NoSuchMethodException {
        thenValidCommand(ManagePresenceRewardUseCase.class, "applyPresenceReward", 0,
                ManagePresenceRewardUseCase.PresenceApplyCommand.class);
        thenValidCommand(ManageCommandUseCase.class, "createCommand", 0,
                ManageCommandUseCase.CreateCommand.class);
        thenValidCommand(ManageCommandUseCase.class, "updateCommand", 1,
                Long.class, ManageCommandUseCase.UpdateCommand.class);
        thenValidCommand(ManageCommandUseCase.class, "preview", 0,
                ManageCommandUseCase.PreviewCommand.class);
        thenValidCommand(AdjustPointUseCase.class, "adjust", 0,
                AdjustPointUseCase.AdjustPointCommand.class);
        thenValidCommand(GrantPointUseCase.class, "grant", 0,
                AdjustPointUseCase.AdjustPointCommand.class);
        thenValidCommand(ReconcilePointBalanceUseCase.class, "reconcileToBalance", 0,
                ReconcilePointBalanceUseCase.ReconcilePointBalanceCommand.class);
        thenValidCommand(ManagePointAdjustmentPresetUseCase.class, "createPreset", 0,
                ManagePointAdjustmentPresetUseCase.CreatePointAdjustmentPreset.class);
        thenValidCommand(ManagePointAdjustmentPresetUseCase.class, "applyAdjustments", 0,
                ManagePointAdjustmentPresetUseCase.ApplyPointAdjustments.class);
        thenValidCommand(ManageRouletteUseCase.class, "createConfig", 0,
                ManageRouletteUseCase.CreateRouletteConfigCommand.class);
        thenValidCommand(ManageRouletteUseCase.class, "addOption", 0,
                ManageRouletteUseCase.AddRouletteOptionCommand.class);
    }

    private void thenValidCommand(
            Class<?> useCase,
            String methodName,
            int commandParameterIndex,
            Class<?>... parameterTypes
    ) throws NoSuchMethodException {
        Method method = useCase.getMethod(methodName, parameterTypes);
        var parameter = method.getParameters()[commandParameterIndex];

        then(parameter.isAnnotationPresent(Valid.class))
                .as(useCase.getSimpleName() + "." + methodName + " @Valid")
                .isTrue();
        then(parameter.isAnnotationPresent(NotNull.class))
                .as(useCase.getSimpleName() + "." + methodName + " @NotNull")
                .isTrue();
    }
}
