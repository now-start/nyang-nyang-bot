package org.nowstart.nyangnyangbot.application.port.out;

import static org.assertj.core.api.BDDAssertions.then;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.nowstart.nyangnyangbot.application.port.out.authorization.AuthorizationPort.SaveAuthorizationCommand;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.AuthorizationToken;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.AuthorizationTokenCommand;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.MessageCommand;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.UserResult;
import org.nowstart.nyangnyangbot.application.port.out.command.CommandPort.CommandRecord;
import org.nowstart.nyangnyangbot.application.port.out.command.CommandPort.CreateData;
import org.nowstart.nyangnyangbot.application.validation.outbound.OutboundResult;
import org.nowstart.nyangnyangbot.application.port.out.donation.DonationPort.SaveDonationCommand;
import org.nowstart.nyangnyangbot.application.port.out.favorite.FavoriteAdjustmentPort.OptionResult;
import org.nowstart.nyangnyangbot.application.port.out.favorite.FavoriteQueryPort.SummaryResult;
import org.nowstart.nyangnyangbot.application.port.out.google.GoogleSheetPort.GoogleSheetRow;
import org.nowstart.nyangnyangbot.application.port.out.overlay.OverlayDisplayPort.DisplayEventResult;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.CreateRouletteEventCommand;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.TableResult;
import org.nowstart.nyangnyangbot.application.port.out.upbo.UpboPort.CreateUserUpboCommand;
import org.nowstart.nyangnyangbot.application.port.out.upbo.UpboPort.TemplateResult;
import org.nowstart.nyangnyangbot.application.port.out.weekly.WeeklyChatRankPort.WeeklyChatRankRecordResult;

class OutboundPortContractTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void outboundRequests_ShouldDeclareRequiredFields() {
        then(messages(new AuthorizationTokenCommand(null, null, null, null, null, null)))
                .contains("grantType is required", "clientId is required", "clientSecret is required");
        then(messages(new MessageCommand(" "))).contains("message is required");
        then(messages(new SaveAuthorizationCommand(" ", null, null, null, null, null, null)))
                .contains("channelId is required", "accessToken is required", "refreshToken is required");
        then(messages(new CreateData(null, null, null, null, null, null, false, null, null, null, null)))
                .contains("type is required", "requiredRole is required", "createdBy is required");
        then(messages(new SaveDonationCommand(null, null, null, null, null, null, null, null)))
                .contains("payAmount is required");
        then(messages(new CreateRouletteEventCommand(
                null, null, null, null, null, null, null, null, null, null, null, null, null
        ))).contains("rouletteTableId is required", "pricePerRound is required", "status is required");
        then(messages(new CreateUserUpboCommand(
                null, null, null, null, null, null, null, null, null, null, null, null, null
        ))).contains("userId is required", "label is required", "status is required");
    }

    @Test
    void externalResults_ShouldDeclareRequiredFields() {
        then(messages(new AuthorizationToken(null, null, null, 0, null)))
                .contains("accessToken is required", "refreshToken is required", "expiresIn must be positive");
        then(messages(new UserResult(" ", " ", null)))
                .contains("channelId is required", "channelName is required");
        then(messages(new GoogleSheetRow(" ", " ", null)))
                .contains("nickName is required", "userId is required", "favorite is required");
    }

    @Test
    void persistedResults_ShouldRequireGeneratedIdentityOnlyAfterPersistence() {
        WeeklyChatRankRecordResult unsaved = new WeeklyChatRankRecordResult(
                null, LocalDate.of(2026, 7, 13), "user-1", "치즈냥", 1L
        );

        then(messages(unsaved)).isEmpty();
        then(validator.validate(unsaved, OutboundResult.class))
                .extracting(violation -> violation.getMessage())
                .contains("id is required");

        then(resultMessages(new CommandRecord(
                null, null, null, null, null, null, null, false, null, null, null, null, null, null
        ))).contains("id is required", "type is required");
        then(resultMessages(new OptionResult(null, null, null)))
                .contains("id is required", "amount is required", "label is required");
        then(resultMessages(new SummaryResult(null, null, null)))
                .contains("userId is required", "favorite is required");
        then(resultMessages(new DisplayEventResult(null, null, null, null, null, null)))
                .contains("id is required", "rouletteEventId is required", "rounds is required");
        then(resultMessages(new TableResult(null, null, null, null, false, null, null)))
                .contains("id is required", "title is required", "pricePerRound is required");
        then(resultMessages(new TemplateResult(null, null, null, false, null, null, null, null)))
                .contains("id is required", "label is required");
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
