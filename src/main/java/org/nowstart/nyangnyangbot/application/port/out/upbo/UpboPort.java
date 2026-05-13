package org.nowstart.nyangnyangbot.application.port.out.upbo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.nowstart.nyangnyangbot.domain.favorite.FavoriteSourceType;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
import org.nowstart.nyangnyangbot.domain.type.UpboStatus;

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
            Long id,
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
            Long id,
            String userId,
            Long upboTemplateId,
            String nickNameSnapshot,
            String label,
            UpboStatus status,
            Integer exchangeFavoriteValue,
            RewardType rewardType,
            ConversionMode conversionMode,
            FavoriteSourceType sourceType,
            Long ledgerId,
            String publicDescription,
            String privateMemo,
            String actorId,
            LocalDateTime createdAt
    ) {
    }

    record CreateUserUpboCommand(
            String userId,
            Long upboTemplateId,
            String nickNameSnapshot,
            String label,
            UpboStatus status,
            Integer exchangeFavoriteValue,
            RewardType rewardType,
            ConversionMode conversionMode,
            FavoriteSourceType sourceType,
            Long ledgerId,
            String publicDescription,
            String privateMemo,
            String actorId
    ) {
    }
}
