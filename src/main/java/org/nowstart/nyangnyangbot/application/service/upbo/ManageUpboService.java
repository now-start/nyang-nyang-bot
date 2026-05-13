package org.nowstart.nyangnyangbot.application.service.upbo;

import jakarta.transaction.Transactional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.application.port.in.favorite.AdjustFavoriteUseCase;
import org.nowstart.nyangnyangbot.application.port.in.favorite.AdjustFavoriteUseCase.AdjustFavoriteCommand;
import org.nowstart.nyangnyangbot.application.port.in.favorite.AdjustFavoriteUseCase.FavoriteLedgerResult;
import org.nowstart.nyangnyangbot.application.port.in.upbo.ManageUpboUseCase;
import org.nowstart.nyangnyangbot.application.port.in.upbo.ManageUpboUseCase.UpboApplyCommand;
import org.nowstart.nyangnyangbot.application.port.in.upbo.ManageUpboUseCase.UpboTemplateCreateCommand;
import org.nowstart.nyangnyangbot.application.port.in.upbo.ManageUpboUseCase.UpboTemplateResult;
import org.nowstart.nyangnyangbot.application.port.in.upbo.ManageUpboUseCase.UserUpboResult;
import org.nowstart.nyangnyangbot.application.port.out.upbo.UpboPort;
import org.nowstart.nyangnyangbot.application.port.out.upbo.UpboPort.CreateUserUpboCommand;
import org.nowstart.nyangnyangbot.application.port.out.upbo.UpboPort.TemplateResult;
import org.nowstart.nyangnyangbot.application.port.out.upbo.UpboPort.UserResult;
import org.nowstart.nyangnyangbot.domain.favorite.FavoriteSourceType;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
import org.nowstart.nyangnyangbot.domain.type.UpboStatus;
import org.nowstart.nyangnyangbot.domain.upbo.UpboPolicy;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class ManageUpboService implements ManageUpboUseCase {

    private final UpboPolicy upboPolicy = new UpboPolicy();
    private final UpboPort upboPort;
    private final AdjustFavoriteUseCase adjustFavoriteUseCase;

    @Override
    public List<UpboTemplateResult> getActiveTemplates() {
        return upboPort.findActiveTemplates().stream()
                .map(this::upboTemplateResult)
                .toList();
    }

    @Override
    public UpboTemplateResult createTemplate(UpboTemplateCreateCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("request is required");
        }
        RewardType rewardType = parseRewardType(command.rewardType());
        ConversionMode conversionMode = parseConversionMode(command.conversionMode());
        upboPolicy.validateTemplate(
                command.label(),
                rewardType,
                conversionMode,
                command.exchangeFavoriteValue()
        );
        TemplateResult template = upboPort.createTemplate(
                command.label().trim(),
                trimToEmpty(command.description()),
                command.displayOrder() == null ? 0 : command.displayOrder(),
                command.exchangeFavoriteValue(),
                rewardType,
                conversionMode
        );
        return upboTemplateResult(template);
    }

    @Override
    public UserUpboResult applyUpbo(UpboApplyCommand command, String actorId) {
        if (command == null) {
            throw new IllegalArgumentException("request is required");
        }
        TemplateResult template = resolveTemplate(command.templateId());
        RewardType rewardType = template == null ? parseRewardType(command.rewardType()) : template.rewardType();
        ConversionMode conversionMode = template == null
                ? parseConversionMode(command.conversionMode())
                : template.conversionMode();
        Integer exchangeFavoriteValue = template == null
                ? command.exchangeFavoriteValue()
                : template.exchangeFavoriteValue();
        upboPolicy.validateApply(
                command.userId(),
                template != null,
                command.label(),
                rewardType,
                conversionMode,
                exchangeFavoriteValue,
                command.publicDescription(),
                command.privateMemo()
        );

        String publicDescription = command.publicDescription().trim();
        String privateMemo = command.privateMemo().trim();
        Long ledgerId = null;
        if (upboPolicy.requiresFavoriteAdjustment(conversionMode)) {
            FavoriteLedgerResult result = adjustFavoriteUseCase.adjust(AdjustFavoriteCommand.builder()
                    .userId(command.userId())
                    .nickName(command.nickName())
                    .delta(exchangeFavoriteValue)
                    .sourceType(FavoriteSourceType.UPBO_MANUAL)
                    .displayCategory("UPBO")
                    .publicDescription(publicDescription)
                    .privateMemo(privateMemo)
                    .actorId(actorId)
                    .allowNegativeBalance(true)
                    .createIfMissing(true)
                    .build());
            ledgerId = result.ledgerId();
        }

        UpboStatus status = upboPolicy.initialStatus(conversionMode);
        UserResult userUpbo = upboPort.createUserUpbo(new CreateUserUpboCommand(
                command.userId(),
                template == null ? null : template.id(),
                trimToEmpty(command.nickName()),
                template == null ? command.label().trim() : template.label(),
                status,
                exchangeFavoriteValue,
                rewardType,
                conversionMode,
                FavoriteSourceType.UPBO_MANUAL,
                ledgerId,
                publicDescription,
                privateMemo,
                actorId
        ));
        return userUpboResult(userUpbo);
    }

    private TemplateResult resolveTemplate(Long templateId) {
        if (templateId == null) {
            return null;
        }
        return upboPort.findTemplateById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("upbo template not found"));
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private RewardType parseRewardType(String value) {
        if (isBlank(value)) {
            return null;
        }
        return RewardType.valueOf(value.trim());
    }

    private ConversionMode parseConversionMode(String value) {
        if (isBlank(value)) {
            return null;
        }
        return ConversionMode.valueOf(value.trim());
    }

    private UpboTemplateResult upboTemplateResult(TemplateResult template) {
        return new UpboTemplateResult(
                template.id(),
                template.label(),
                template.description(),
                template.active(),
                template.displayOrder(),
                template.exchangeFavoriteValue(),
                template.rewardType() == null ? null : template.rewardType().name(),
                template.conversionMode() == null ? null : template.conversionMode().name()
        );
    }

    private UserUpboResult userUpboResult(UserResult userUpbo) {
        return new UserUpboResult(
                userUpbo.id(),
                userUpbo.userId(),
                userUpbo.nickNameSnapshot(),
                userUpbo.label(),
                userUpbo.status() == null ? null : userUpbo.status().name(),
                userUpbo.exchangeFavoriteValue(),
                userUpbo.rewardType() == null ? null : userUpbo.rewardType().name(),
                userUpbo.conversionMode() == null ? null : userUpbo.conversionMode().name(),
                userUpbo.ledgerId(),
                userUpbo.publicDescription(),
                userUpbo.createdAt()
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
