package org.nowstart.nyangnyangbot.architecture;

import static org.assertj.core.api.BDDAssertions.then;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.nowstart.nyangnyangbot.application.port.in.chat.HandleChatEventUseCase;
import org.nowstart.nyangnyangbot.application.port.in.chzzk.HandleChzzkSystemEventUseCase;
import org.nowstart.nyangnyangbot.application.port.in.command.ExecuteCommandUseCase;
import org.nowstart.nyangnyangbot.application.port.in.command.ManageCommandUseCase;
import org.nowstart.nyangnyangbot.application.port.in.donation.HandleDonationEventUseCase;
import org.nowstart.nyangnyangbot.application.port.in.point.AdjustPointUseCase;
import org.nowstart.nyangnyangbot.application.port.in.point.GrantPointUseCase;
import org.nowstart.nyangnyangbot.application.port.in.point.ManagePointAdjustmentPresetUseCase;
import org.nowstart.nyangnyangbot.application.port.in.point.ReconcilePointBalanceUseCase;
import org.nowstart.nyangnyangbot.application.port.in.presence.ManagePresenceRewardUseCase;
import org.nowstart.nyangnyangbot.application.port.in.presence.RecordPresenceChatUseCase;
import org.nowstart.nyangnyangbot.application.port.in.reward.GrantRouletteRewardUseCase;
import org.nowstart.nyangnyangbot.application.port.in.roulette.ManageRouletteUseCase;
import org.nowstart.nyangnyangbot.application.port.in.roulette.ProcessRouletteDonationUseCase;
import org.nowstart.nyangnyangbot.application.port.in.weeklychat.RecordWeeklyChatUseCase;
import org.nowstart.nyangnyangbot.application.service.chat.ChatService;
import org.nowstart.nyangnyangbot.application.service.chzzk.SystemService;
import org.nowstart.nyangnyangbot.application.service.command.CommandExecutionService;
import org.nowstart.nyangnyangbot.application.service.command.CommandService;
import org.nowstart.nyangnyangbot.application.service.donation.DonationService;
import org.nowstart.nyangnyangbot.application.service.overlay.OverlayDisplayService;
import org.nowstart.nyangnyangbot.application.service.overlay.OverlayTokenService;
import org.nowstart.nyangnyangbot.application.service.point.PointAdjustmentPresetService;
import org.nowstart.nyangnyangbot.application.service.point.PointLedgerService;
import org.nowstart.nyangnyangbot.application.service.point.PointQueryService;
import org.nowstart.nyangnyangbot.application.service.presence.PresenceRewardService;
import org.nowstart.nyangnyangbot.application.service.reward.RewardService;
import org.nowstart.nyangnyangbot.application.service.roulette.ManageRouletteService;
import org.nowstart.nyangnyangbot.application.service.roulette.ProcessRouletteDonationService;
import org.nowstart.nyangnyangbot.application.service.roulette.QueryRouletteResultService;
import org.nowstart.nyangnyangbot.application.service.timer.TimerMessageService;
import org.nowstart.nyangnyangbot.application.service.user.UserAccountService;
import org.nowstart.nyangnyangbot.application.service.weeklychat.WeeklyChatRankService;
import org.springframework.validation.annotation.Validated;

class ApplicationValidationBoundaryTest {

    @Test
    void inboundPortServices_ShouldEnableMethodValidation() {
        List<Class<?>> services = List.of(
                PresenceRewardService.class,
                CommandService.class,
                CommandExecutionService.class,
                ChatService.class,
                DonationService.class,
                SystemService.class,
                OverlayDisplayService.class,
                OverlayTokenService.class,
                PointAdjustmentPresetService.class,
                PointLedgerService.class,
                PointQueryService.class,
                ManageRouletteService.class,
                ProcessRouletteDonationService.class,
                QueryRouletteResultService.class,
                RewardService.class,
                TimerMessageService.class,
                UserAccountService.class,
                WeeklyChatRankService.class
        );

        then(services).allSatisfy(service ->
                then(service.isAnnotationPresent(Validated.class))
                        .as(service.getSimpleName())
                        .isTrue());
    }

    @Test
    void commandAndEventParameters_ShouldDeclareCascadedValidation() throws NoSuchMethodException {
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
        thenValidCommand(HandleChatEventUseCase.class, "handle", 0,
                HandleChatEventUseCase.ChatReceived.class);
        thenValidCommand(HandleChzzkSystemEventUseCase.class, "handle", 1,
                long.class, HandleChzzkSystemEventUseCase.SystemReceived.class);
        thenValidCommand(ExecuteCommandUseCase.class, "execute", 0,
                ExecuteCommandUseCase.ExecuteCommand.class);
        thenValidCommand(HandleDonationEventUseCase.class, "handle", 0,
                HandleDonationEventUseCase.DonationReceived.class);
        thenValidCommand(GrantRouletteRewardUseCase.class, "grantRoulette", 0,
                GrantRouletteRewardUseCase.RouletteRewardCommand.class);
        thenValidCommand(ProcessRouletteDonationUseCase.class, "processDonation", 1,
                Long.class, HandleDonationEventUseCase.DonationReceived.class);
        thenValidCommand(RecordPresenceChatUseCase.class, "recordChatUser", 0,
                HandleChatEventUseCase.ChatReceived.class);
        thenValidCommand(RecordWeeklyChatUseCase.class, "recordChat", 0,
                HandleChatEventUseCase.ChatReceived.class);
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
