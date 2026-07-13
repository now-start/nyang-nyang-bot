package org.nowstart.nyangnyangbot.architecture;

import static org.assertj.core.api.BDDAssertions.then;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.nowstart.nyangnyangbot.application.port.in.attendance.ManageAttendanceUseCase;
import org.nowstart.nyangnyangbot.application.port.in.command.ManageCommandUseCase;
import org.nowstart.nyangnyangbot.application.port.in.favorite.AdjustFavoriteUseCase;
import org.nowstart.nyangnyangbot.application.port.in.favorite.CorrectFavoriteLedgerUseCase;
import org.nowstart.nyangnyangbot.application.port.in.favorite.GrantFavoriteUseCase;
import org.nowstart.nyangnyangbot.application.port.in.favorite.ManageFavoriteAdjustmentUseCase;
import org.nowstart.nyangnyangbot.application.port.in.roulette.ManageRouletteUseCase;
import org.nowstart.nyangnyangbot.application.port.in.upbo.ManageUpboUseCase;
import org.nowstart.nyangnyangbot.application.service.attendance.AttendanceService;
import org.nowstart.nyangnyangbot.application.service.command.CommandService;
import org.nowstart.nyangnyangbot.application.service.favorite.FavoriteAdjustmentService;
import org.nowstart.nyangnyangbot.application.service.favorite.FavoriteLedgerService;
import org.nowstart.nyangnyangbot.application.service.roulette.ManageRouletteService;
import org.nowstart.nyangnyangbot.application.service.upbo.ManageUpboService;
import org.springframework.validation.annotation.Validated;

class ApplicationValidationBoundaryTest {

    @Test
    void commandHandlingServices_ShouldEnableMethodValidation() {
        List<Class<?>> services = List.of(
                AttendanceService.class,
                CommandService.class,
                FavoriteAdjustmentService.class,
                FavoriteLedgerService.class,
                ManageRouletteService.class,
                ManageUpboService.class
        );

        then(services).allSatisfy(service ->
                then(service.isAnnotationPresent(Validated.class))
                        .as(service.getSimpleName())
                        .isTrue());
    }

    @Test
    void commandParameters_ShouldDeclareCascadedValidation() throws NoSuchMethodException {
        thenValidCommand(ManageAttendanceUseCase.class, "applyAttendance", 0,
                ManageAttendanceUseCase.AttendanceApplyCommand.class);
        thenValidCommand(ManageCommandUseCase.class, "createCommand", 0,
                ManageCommandUseCase.CreateCommand.class);
        thenValidCommand(ManageCommandUseCase.class, "updateCommand", 1,
                Long.class, ManageCommandUseCase.UpdateCommand.class);
        thenValidCommand(ManageCommandUseCase.class, "preview", 0,
                ManageCommandUseCase.PreviewCommand.class);
        thenValidCommand(AdjustFavoriteUseCase.class, "adjust", 0,
                AdjustFavoriteUseCase.AdjustFavoriteCommand.class);
        thenValidCommand(GrantFavoriteUseCase.class, "grant", 0,
                AdjustFavoriteUseCase.AdjustFavoriteCommand.class);
        thenValidCommand(CorrectFavoriteLedgerUseCase.class, "correct", 0,
                AdjustFavoriteUseCase.AdjustFavoriteCommand.class);
        thenValidCommand(ManageFavoriteAdjustmentUseCase.class, "createAdjustment", 0,
                ManageFavoriteAdjustmentUseCase.FavoriteAdjustmentCreateCommand.class);
        thenValidCommand(ManageFavoriteAdjustmentUseCase.class, "applyAdjustments", 0,
                ManageFavoriteAdjustmentUseCase.FavoriteAdjustmentApplyCommand.class);
        thenValidCommand(ManageRouletteUseCase.class, "createTable", 0,
                ManageRouletteUseCase.CreateRouletteTableCommand.class);
        thenValidCommand(ManageRouletteUseCase.class, "addItem", 0,
                ManageRouletteUseCase.AddRouletteItemCommand.class);
        thenValidCommand(ManageUpboUseCase.class, "createTemplate", 0,
                ManageUpboUseCase.UpboTemplateCreateCommand.class);
        thenValidCommand(ManageUpboUseCase.class, "applyUpbo", 0,
                ManageUpboUseCase.UpboApplyCommand.class, String.class);
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
