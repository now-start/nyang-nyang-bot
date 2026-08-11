package org.nowstart.nyangnyangbot.application.port.in;

import static org.assertj.core.api.BDDAssertions.then;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.nowstart.nyangnyangbot.application.port.in.chat.HandleChatEventUseCase.ChatReceived;
import org.nowstart.nyangnyangbot.application.port.in.chzzk.HandleChzzkSystemEventUseCase.SystemReceived;
import org.nowstart.nyangnyangbot.application.port.in.command.ExecuteCommandUseCase.ExecuteCommand;
import org.nowstart.nyangnyangbot.application.port.in.donation.HandleDonationEventUseCase.DonationReceived;
import org.nowstart.nyangnyangbot.application.port.in.reward.GrantRouletteRewardUseCase.RouletteRewardCommand;

class InboundPortValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void chatEvent_ShouldRequireChannelId() {
        ChatReceived event = new ChatReceived(" ", null, null, null, null, null);

        then(messages(event)).containsExactly("channelId is required");
    }

    @Test
    void donationEvent_ShouldRequireCanonicalIdentityAndAmount() {
        DonationReceived event = new DonationReceived(
                " ", null, " ", null, null, " ", null, Map.of()
        );

        then(messages(event)).containsExactlyInAnyOrder(
                "ingestionKey is required",
                "channelId is required",
                "payAmount is required"
        );
    }

    @Test
    void systemEvent_ShouldRequireTypeAndData() {
        SystemReceived event = new SystemReceived(" ", null);

        then(messages(event)).containsExactlyInAnyOrder(
                "type is required",
                "system data is required"
        );
    }

    @Test
    void executeCommand_ShouldRequireTriggerAndUser() {
        ExecuteCommand command = new ExecuteCommand(" ", " ", null, null, null, null);

        then(messages(command)).containsExactlyInAnyOrder(
                "trigger is required",
                "userId is required"
        );
    }

    @Test
    void rouletteReward_ShouldRequireStableIdentityAndRewardTypes() {
        RouletteRewardCommand command = new RouletteRewardCommand(
                null, " ", null, " ", " ", null, null, null, null, null
        );

        then(messages(command)).containsExactlyInAnyOrder(
                "roundId is required",
                "userId is required",
                "ingestionKey is required",
                "label is required",
                "rewardType is required",
                "conversionMode is required"
        );
    }

    private <T> Set<String> messages(T value) {
        return validator.validate(value).stream()
                .map(violation -> violation.getMessage())
                .collect(java.util.stream.Collectors.toSet());
    }
}
