package org.nowstart.nyangnyangbot.application.port.out.upbo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.nowstart.nyangnyangbot.domain.favorite.FavoriteSourceType;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
import org.nowstart.nyangnyangbot.domain.type.UpboStatus;
import org.nowstart.nyangnyangbot.application.validation.outbound.OutboundResult;

public interface UpboPort {

    List<TemplateResult> findActiveTemplates();

    TemplateResult createTemplate(
            String label,
            String description,
            Integer displayOrder,
            Integer exchangeFavoriteValue,
            RewardType rewardType,
            ConversionMode conversionMode
    );

    Optional<TemplateResult> findTemplateById(Long templateId);

    UserResult createUserUpbo(CreateUserUpboCommand command);

    List<UserResult> findUserUpbos(String userId);

    List<UserResult> findUserUpbosByStatus(String userId, UpboStatus status);

    record TemplateResult(
            @NotNull(groups = OutboundResult.class, message = "id is required")
            @Positive(groups = OutboundResult.class, message = "id must be positive")
            Long id,
            @NotBlank(message = "label is required")
            String label,
            String description,
            boolean active,
            Integer displayOrder,
            Integer exchangeFavoriteValue,
            RewardType rewardType,
            ConversionMode conversionMode
    ) {
    }

    record UserResult(
            @NotNull(groups = OutboundResult.class, message = "id is required")
            @Positive(groups = OutboundResult.class, message = "id must be positive")
            Long id,
            @NotBlank(message = "userId is required")
            String userId,
            Long upboTemplateId,
            String nickNameSnapshot,
            @NotBlank(message = "label is required")
            String label,
            @NotNull(message = "status is required")
            UpboStatus status,
            Integer exchangeFavoriteValue,
            RewardType rewardType,
            ConversionMode conversionMode,
            @NotNull(message = "sourceType is required")
            FavoriteSourceType sourceType,
            Long ledgerId,
            String publicDescription,
            String privateMemo,
            String actorId,
            LocalDateTime createdAt
    ) {
    }

    record CreateUserUpboCommand(
            @NotBlank(message = "userId is required")
            String userId,
            Long upboTemplateId,
            String nickNameSnapshot,
            @NotBlank(message = "label is required")
            String label,
            @NotNull(message = "status is required")
            UpboStatus status,
            Integer exchangeFavoriteValue,
            RewardType rewardType,
            ConversionMode conversionMode,
            @NotNull(message = "sourceType is required")
            FavoriteSourceType sourceType,
            Long ledgerId,
            String publicDescription,
            String privateMemo,
            String actorId
    ) {
    }
}
