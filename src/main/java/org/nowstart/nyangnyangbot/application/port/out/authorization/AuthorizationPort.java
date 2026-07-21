package org.nowstart.nyangnyangbot.application.port.out.authorization;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;
import java.util.Optional;

public interface AuthorizationPort {

    Optional<AuthorizationAccountResult> findById(String channelId);

    AuthorizationAccountResult saveOrUpdate(SaveAuthorizationCommand command);

    AuthorizationAccountResult updateToken(String channelId, SaveAuthorizationCommand command);

    record SaveAuthorizationCommand(
            @NotBlank(message = "channelId is required")
            String channelId,
            String channelName,
            @NotBlank(message = "accessToken is required")
            String accessToken,
            @NotBlank(message = "refreshToken is required")
            String refreshToken,
            @NotBlank(message = "tokenType is required")
            String tokenType,
            @NotNull(message = "expiresIn is required")
            @Positive(message = "expiresIn must be positive")
            Integer expiresIn,
            String scope
    ) {
    }

    record AuthorizationAccountResult(
            @NotBlank(message = "channelId is required")
            String channelId,
            String channelName,
            @NotBlank(message = "accessToken is required")
            String accessToken,
            @NotBlank(message = "refreshToken is required")
            String refreshToken,
            @NotBlank(message = "tokenType is required")
            String tokenType,
            @NotNull(message = "expiresIn is required")
            @Positive(message = "expiresIn must be positive")
            Integer expiresIn,
            String scope,
            boolean admin,
            LocalDateTime modifyDate,
            LocalDateTime lastLoginAt
    ) {
    }
}
