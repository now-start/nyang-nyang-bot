package org.nowstart.nyangnyangbot.adapter.out.persistence.upbo;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.application.port.out.upbo.UpboPort.TemplateResult;
import org.nowstart.nyangnyangbot.application.port.out.upbo.UpboPort.UserResult;
import org.nowstart.nyangnyangbot.application.port.out.upbo.UpboPort.CreateUserUpboCommand;
import org.nowstart.nyangnyangbot.application.port.out.upbo.UpboPort;
import org.nowstart.nyangnyangbot.adapter.out.persistence.upbo.entity.UpboTemplateEntity;
import org.nowstart.nyangnyangbot.adapter.out.persistence.upbo.entity.UserUpboEntity;
import org.nowstart.nyangnyangbot.adapter.out.persistence.upbo.repository.UpboTemplateRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.upbo.repository.UserUpboRepository;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
import org.nowstart.nyangnyangbot.domain.type.UpboStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UpboPersistenceAdapter implements UpboPort {

    private final UpboTemplateRepository upboTemplateRepository;
    private final UserUpboRepository userUpboRepository;

    @Override
    public List<TemplateResult> findActiveTemplates() {
        return upboTemplateRepository.findByActiveTrueOrderByDisplayOrderAscIdAsc().stream()
                .map(this::toModel)
                .toList();
    }

    @Override
    public TemplateResult createTemplate(
            String label,
            String description,
            Integer displayOrder,
            Integer exchangeFavoriteValue,
            RewardType rewardType,
            ConversionMode conversionMode
    ) {
        UpboTemplateEntity saved = upboTemplateRepository.save(UpboTemplateEntity.builder()
                .label(label)
                .description(description)
                .active(true)
                .displayOrder(displayOrder)
                .exchangeFavoriteValue(exchangeFavoriteValue)
                .rewardType(rewardType)
                .conversionMode(conversionMode)
                .build());
        return toModel(saved);
    }

    @Override
    public Optional<TemplateResult> findTemplateById(Long templateId) {
        return upboTemplateRepository.findById(templateId).map(this::toModel);
    }

    @Override
    public UserResult createUserUpbo(CreateUserUpboCommand command) {
        UpboTemplateEntity template = command.upboTemplateId() == null
                ? null
                : upboTemplateRepository.getReferenceById(command.upboTemplateId());
        UserUpboEntity saved = userUpboRepository.save(UserUpboEntity.builder()
                .userId(command.userId())
                .upboTemplate(template)
                .nickNameSnapshot(command.nickNameSnapshot())
                .label(command.label())
                .status(command.status())
                .exchangeFavoriteValue(command.exchangeFavoriteValue())
                .rewardType(command.rewardType())
                .conversionMode(command.conversionMode())
                .sourceType(command.sourceType())
                .ledgerId(command.ledgerId())
                .publicDescription(command.publicDescription())
                .privateMemo(command.privateMemo())
                .actorId(command.actorId())
                .build());
        return toModel(saved);
    }

    @Override
    public List<UserResult> findUserUpbos(String userId) {
        return userUpboRepository.findByUserIdOrderByCreateDateDesc(userId).stream()
                .map(this::toModel)
                .toList();
    }

    @Override
    public List<UserResult> findUserUpbosByStatus(String userId, UpboStatus status) {
        return userUpboRepository.findByUserIdAndStatusOrderByCreateDateDesc(userId, status).stream()
                .map(this::toModel)
                .toList();
    }

    private TemplateResult toModel(UpboTemplateEntity entity) {
        return new TemplateResult(
                entity.getId(),
                entity.getLabel(),
                entity.getDescription(),
                entity.isActive(),
                entity.getDisplayOrder(),
                entity.getExchangeFavoriteValue(),
                entity.getRewardType(),
                entity.getConversionMode()
        );
    }

    private UserResult toModel(UserUpboEntity entity) {
        Long templateId = entity.getUpboTemplate() == null ? null : entity.getUpboTemplate().getId();
        return new UserResult(
                entity.getId(),
                entity.getUserId(),
                templateId,
                entity.getNickNameSnapshot(),
                entity.getLabel(),
                entity.getStatus(),
                entity.getExchangeFavoriteValue(),
                entity.getRewardType(),
                entity.getConversionMode(),
                entity.getSourceType(),
                entity.getLedgerId(),
                entity.getPublicDescription(),
                entity.getPrivateMemo(),
                entity.getActorId(),
                entity.getCreateDate()
        );
    }
}
