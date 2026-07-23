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
import org.nowstart.nyangnyangbot.application.port.out.google.GoogleSheetPort.GoogleSheetRow;
import org.nowstart.nyangnyangbot.application.port.out.user.OAuthCredentialPort.SaveOAuthCredential;
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
    void persistedCommandResult_ShouldRequireIdentityOnlyInOutboundResultGroup() {
        then(resultMessages(new CommandRecord(
                null, null, null, false, null, null, null, null, null
        ))).contains("id is required", "trigger is required", "messageTemplate is required");
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
