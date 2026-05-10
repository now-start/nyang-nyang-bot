package org.nowstart.nyangnyangbot.application.gateway.out.upbo;

import java.util.List;
import java.util.Optional;
import org.nowstart.nyangnyangbot.domain.model.UpboTemplate;
import org.nowstart.nyangnyangbot.domain.model.UserUpbo;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
import org.nowstart.nyangnyangbot.domain.type.UpboStatus;

public interface UpboPort {

    List<UpboTemplate> findActiveTemplates();

    UpboTemplate createTemplate(
            String label,
            String description,
            Integer displayOrder,
            Integer exchangeFavoriteValue,
            RewardType rewardType,
            ConversionMode conversionMode
    );

    Optional<UpboTemplate> findTemplateById(Long templateId);

    UserUpbo createUserUpbo(CreateUserUpboCommand command);

    List<UserUpbo> findUserUpbos(String userId);

    List<UserUpbo> findUserUpbosByStatus(String userId, UpboStatus status);
}
