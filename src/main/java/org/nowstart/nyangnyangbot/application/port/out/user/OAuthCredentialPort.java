package org.nowstart.nyangnyangbot.application.port.out.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Instant;
import java.util.Optional;

public interface OAuthCredentialPort {

    /** 사용자가 소유한 OAuth 인증 정보를 조회한다. */
    @Valid
    Optional<OAuthCredentialRecord> findByUserId(String userId);

    /** 사용자의 OAuth 인증 정보를 조회하고 현재 트랜잭션 동안 쓰기 잠금을 유지한다. */
    @Valid
    Optional<OAuthCredentialRecord> findByUserIdForUpdate(String userId);

    /** 데이터베이스 서버의 현재 시각을 반환한다. */
    @NotNull(message = "database time is required")
    Instant currentDatabaseTime();

    /** 로그인을 기록하고 사용자의 OAuth 토큰 값을 생성하거나 교체한다. */
    @Valid
    OAuthCredentialRecord saveLogin(
            @Valid @NotNull(message = "OAuth credential command is required") SaveOAuthCredential command
    );

    /** 저장된 인증 정보 버전이 예상 버전과 일치하는 경우에만 토큰 값을 갱신한다. */
    @Valid
    OAuthCredentialRecord updateToken(
            @NotBlank(message = "userId is required") String userId,
            @PositiveOrZero(message = "expectedCredentialVersion must not be negative") long expectedCredentialVersion,
            @Valid @NotNull(message = "OAuth credential command is required") SaveOAuthCredential command
    );

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
