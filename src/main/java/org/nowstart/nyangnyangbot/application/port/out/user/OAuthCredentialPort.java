package org.nowstart.nyangnyangbot.application.port.out.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.util.Optional;

public interface OAuthCredentialPort {

    Optional<OAuthCredentialRecord> findByUserId(String userId);

    Optional<OAuthCredentialRecord> findByUserIdForUpdate(String userId);

    Instant currentDatabaseTime();

    OAuthCredentialRecord saveLogin(SaveOAuthCredential command);

    OAuthCredentialRecord updateToken(String userId, long expectedCredentialVersion, SaveOAuthCredential command);

    record SaveOAuthCredential(
            @NotBlank(message = "userId is required") String userId,
            String displayName,
            @NotBlank(message = "accessToken is required") String accessToken,
            @NotBlank(message = "refreshToken is required") String refreshToken,
            @NotBlank(message = "tokenType is required") String tokenType,
            @NotNull(message = "expiresIn is required")
            @Positive(message = "expiresIn must be positive") Integer expiresIn,
            String scope
    ) {

        @Override
        public String toString() {
            return "SaveOAuthCredential[userId=%s, displayName=%s, accessToken=<masked>, refreshToken=<masked>, "
                    + "tokenType=%s, expiresIn=%s, scope=%s]"
                    .formatted(userId, displayName, tokenType, expiresIn, scope);
        }
    }

    record OAuthCredentialRecord(
            String userId,
            String displayName,
            String accessToken,
            String refreshToken,
            String tokenType,
            boolean admin,
            Instant accessTokenExpiresAt,
            long credentialVersion
    ) {

        @Override
        public String toString() {
            return "OAuthCredentialRecord[userId=%s, displayName=%s, accessToken=<masked>, refreshToken=<masked>, "
                    + "tokenType=%s, admin=%s, accessTokenExpiresAt=%s, credentialVersion=%s]"
                    .formatted(
                            userId,
                            displayName,
                            tokenType,
                            admin,
                            accessTokenExpiresAt,
                            credentialVersion
                    );
        }
    }
}
