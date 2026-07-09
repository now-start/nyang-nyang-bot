package org.nowstart.nyangnyangbot.adapter.out.persistence.upbo;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.adapter.out.persistence.upbo.entity.UpboTemplate;
import org.nowstart.nyangnyangbot.adapter.out.persistence.upbo.entity.UserUpbo;
import org.nowstart.nyangnyangbot.adapter.out.persistence.upbo.repository.UpboTemplateRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.upbo.repository.UserUpboRepository;
import org.nowstart.nyangnyangbot.application.port.out.upbo.UpboPort;
import org.nowstart.nyangnyangbot.config.cache.CacheNames;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
import org.nowstart.nyangnyangbot.domain.type.UpboStatus;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UpboPersistenceAdapter implements UpboPort {

    private final UpboTemplateRepository upboTemplateRepository;
    private final UserUpboRepository userUpboRepository;
    private final UpboPersistenceMapper mapper;

    @Override
    @Cacheable(cacheNames = CacheNames.UPBO_ACTIVE_TEMPLATES)
    public List<TemplateResult> findActiveTemplates() {
        return upboTemplateRepository.findByActiveTrueOrderByDisplayOrderAscIdAsc().stream()
                .map(mapper::templateResult)
                .toList();
    }

    @Override
    @CacheEvict(cacheNames = CacheNames.UPBO_ACTIVE_TEMPLATES, allEntries = true)
    public TemplateResult createTemplate(
            String label,
            String description,
            Integer displayOrder,
            Integer exchangeFavoriteValue,
            RewardType rewardType,
            ConversionMode conversionMode
    ) {
        UpboTemplate saved = upboTemplateRepository.save(UpboTemplate.builder()
                .label(label)
                .description(description)
                .active(true)
                .displayOrder(displayOrder)
                .exchangeFavoriteValue(exchangeFavoriteValue)
                .rewardType(rewardType)
                .conversionMode(conversionMode)
                .build());
        return mapper.templateResult(saved);
    }

    @Override
    @Cacheable(cacheNames = CacheNames.UPBO_TEMPLATE_BY_ID, key = "#templateId", unless = "#result == null")
    public Optional<TemplateResult> findTemplateById(Long templateId) {
        return upboTemplateRepository.findById(templateId).map(mapper::templateResult);
    }

    @Override
    public UserResult createUserUpbo(CreateUserUpboCommand command) {
        UpboTemplate template = command.upboTemplateId() == null
                ? null
                : upboTemplateRepository.getReferenceById(command.upboTemplateId());
        UserUpbo saved = userUpboRepository.save(UserUpbo.builder()
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
        return mapper.userResult(saved);
    }

    @Override
    public List<UserResult> findUserUpbos(String userId) {
        return userUpboRepository.findByUserIdOrderByCreateDateDesc(userId).stream()
                .map(mapper::userResult)
                .toList();
    }

    @Override
    public List<UserResult> findUserUpbosByStatus(String userId, UpboStatus status) {
        return userUpboRepository.findByUserIdAndStatusOrderByCreateDateDesc(userId, status).stream()
                .map(mapper::userResult)
                .toList();
    }
}
