package org.nowstart.nyangnyangbot.application.port.out;

import static org.assertj.core.api.BDDAssertions.then;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.AuthorizationToken;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.AuthorizationTokenCommand;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.MessageCommand;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.UserResult;
import org.nowstart.nyangnyangbot.application.port.out.command.CommandPort.CommandRecord;
import org.nowstart.nyangnyangbot.application.port.out.command.CommandPort.CreateData;
import org.nowstart.nyangnyangbot.application.port.out.donation.DonationPort.SaveDonationCommand;
import org.nowstart.nyangnyangbot.application.port.out.google.GoogleSheetPort.GoogleSheetRow;
import org.nowstart.nyangnyangbot.application.port.out.overlay.OverlayDisplayPort.DisplayJobResult;
import org.nowstart.nyangnyangbot.application.port.out.point.PointLedgerPort.AppendPointEntry;
import org.nowstart.nyangnyangbot.application.port.out.point.PointAdjustmentPresetPort.SavePresetCommand;
import org.nowstart.nyangnyangbot.application.port.out.reward.RewardPort.CreateRewardCommand;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.ConfigResult;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.CreateConfigCommand;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.CreateRoundCommand;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.CreateRunCommand;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RecentRouletteResultQueryPort.RecentRound;
import org.nowstart.nyangnyangbot.application.port.out.timer.TimerMessagePort;
import org.nowstart.nyangnyangbot.application.port.out.user.OAuthCredentialPort.SaveOAuthCredential;
import org.nowstart.nyangnyangbot.application.port.out.user.UserAccountPort.ObserveUserCommand;
import org.nowstart.nyangnyangbot.application.port.out.weekly.WeeklyChatCountPort.IncrementWeeklyChatCommand;
import org.nowstart.nyangnyangbot.application.port.out.weekly.WeeklyChatCountPort.WeeklyChatRankRecord;
import org.nowstart.nyangnyangbot.application.validation.outbound.OutboundResult;

class OutboundPortContractTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void outboundRequests_ShouldDeclareRequiredFields() {
        then(messages(new AuthorizationTokenCommand(null, null, null, null, null, null)))
                .contains("grantType is required", "clientId is required", "clientSecret is required");
        then(messages(new MessageCommand(" "))).contains("message is required");
        then(messages(new SaveOAuthCredential(" ", null, null, null, null, null, null)))
                .contains("userId is required", "accessToken is required", "refreshToken is required");
        then(messages(new CreateData(null, null, false, null, null, null)))
                .contains("trigger is required", "messageTemplate is required");
        then(messages(new SaveDonationCommand(null, null, null, null, null, -1, null, null)))
                .contains(
                        "ingestionKey is required",
                        "donationType is required",
                        "recipientUserId is required",
                        "amount must not be negative",
                        "receivedAt is required"
                );
        then(messages(new AppendPointEntry(null, 0, null, null, null, null, null, null, null)))
                .contains(
                        "userId is required",
                        "delta must not be zero",
                        "sourceType is required",
                        "description is required",
                        "idempotencyKey is required"
                );
        then(messages(new CreateRewardCommand(
                null, null, null, null, null, null, null, null, null, null, null, null, null
        ))).contains(
                "userId is required",
                "rouletteRoundId is required",
                "label is required",
                "rewardType is required",
                "conversionMode is required",
                "status is required",
                "idempotencyKey is required",
                "createdAt is required"
        );
        then(messages(new CreateConfigCommand(null, null, null, null, null)))
                .contains(
                        "title is required",
                        "triggerToken is required",
                        "pricePerRound is required",
                        "highRoundThreshold is required",
                        "createdAt is required"
                );
        then(messages(new SavePresetCommand(0, null)))
                .contains("amount must not be zero", "label is required");
        then(messages(new ObserveUserCommand(null, null))).contains("userId is required");
        then(messages(new IncrementWeeklyChatCommand(null, null)))
                .contains("weekStartedAt is required", "userId is required");
        then(messages(new CreateRunCommand(
                null,
                null,
                null,
                java.util.List.of(new CreateRoundCommand(null, 0, 10_001))
        ))).contains(
                "donationId is required",
                "configId is required",
                "createdAt is required",
                "optionId is required",
                "roundNo must be positive",
                "ticket must not exceed 10000"
        );
    }

    @Test
    void externalResults_ShouldDeclareRequiredFields() {
        then(messages(new AuthorizationToken(null, null, null, 0, null)))
                .contains("accessToken is required", "refreshToken is required", "expiresIn must be positive");
        then(messages(new UserResult(" ", " ", null)))
                .contains("channelId is required", "channelName is required");
        then(messages(new GoogleSheetRow(" ", " ", null)))
                .contains("displayName is required", "userId is required", "point is required");
    }

    @Test
    void outboundContracts_ShouldPreserveDomainRanges() {
        then(messages(new CreateData(
                "a",
                "message",
                false,
                org.nowstart.nyangnyangbot.domain.command.CommandExecutionPolicy.USER_INTERVAL,
                1,
                null,
                null
        ))).contains(
                "trigger length must be between 2 and 20",
                "userCooldownSeconds must be between 5 and 3600"
        );
        then(messages(new TimerMessagePort.CreateData("message", 1, 0, true, null, null, null)))
                .contains(
                        "intervalMinutes must be between 5 and 1440",
                        "minChatCount must be between 1 and 10000"
                );
    }

    @Test
    void persistedCommandResult_ShouldRequireIdentityOnlyInOutboundResultGroup() {
        then(resultMessages(new CommandRecord(
                null, null, null, false, null, null, null
        ))).contains("id is required", "trigger is required", "messageTemplate is required");

        then(resultMessages(new ConfigResult(null, null, null, null, null, null, null, null)))
                .contains(
                        "id is required",
                        "title is required",
                        "triggerToken is required",
                        "pricePerRound is required",
                        "highRoundThreshold is required",
                        "status is required",
                        "createdAt is required",
                        "updatedAt is required"
                );
        then(resultMessages(new DisplayJobResult(null, null, null, -1, null)))
                .contains(
                        "id is required",
                        "claimToken is required",
                        "roundCount must not be negative",
                        "rounds are required"
                );
        then(resultMessages(new WeeklyChatRankRecord(0, null, -1L)))
                .contains("rank must be positive", "chatCount must not be negative");
        then(resultMessages(new WeeklyChatRankRecord(null, null, null)))
                .contains("rank is required", "chatCount is required");
        then(resultMessages(new RecentRound(null, null)))
                .contains("roundNo is required", "itemLabel is required");
    }

    private Set<String> messages(Object value) {
        return validator.validate(value).stream()
                .map(violation -> violation.getMessage())
                .collect(java.util.stream.Collectors.toSet());
    }

    private Set<String> resultMessages(Object value) {
        return validator.validate(value, jakarta.validation.groups.Default.class, OutboundResult.class).stream()
                .map(violation -> violation.getMessage())
                .collect(java.util.stream.Collectors.toSet());
    }
}
