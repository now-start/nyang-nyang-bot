package org.nowstart.nyangnyangbot.application.port.out;

import static org.assertj.core.api.BDDAssertions.then;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.nowstart.nyangnyangbot.application.port.out.user.OAuthCredentialPort.OAuthCredentialRecord;
import org.nowstart.nyangnyangbot.application.port.out.user.OAuthCredentialPort.SaveOAuthCredential;

class OAuthCredentialPortTest {

    @Test
    void oauthRecordsMaskTokensInToString() {
        SaveOAuthCredential command = new SaveOAuthCredential(
                "user-1",
                "냥이",
                "secret-access-token",
                "secret-refresh-token",
                "Bearer",
                3600,
                "scope"
        );
        OAuthCredentialRecord record = new OAuthCredentialRecord(
                "user-1",
                "냥이",
                "secret-access-token",
                "secret-refresh-token",
                "Bearer",
                false,
                Instant.parse("2026-07-23T01:00:00Z"),
                1
        );

        then(command.toString())
                .doesNotContain("secret-access-token", "secret-refresh-token")
                .contains("accessToken=<masked>", "refreshToken=<masked>");
        then(record.toString())
                .doesNotContain("secret-access-token", "secret-refresh-token")
                .contains("accessToken=<masked>", "refreshToken=<masked>");
    }
}
