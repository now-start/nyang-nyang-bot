package org.nowstart.nyangnyangbot.application.service.upbo;

import jakarta.transaction.Transactional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.application.port.in.favorite.dto.AdjustFavoriteCommand;
import org.nowstart.nyangnyangbot.application.port.in.favorite.dto.FavoriteLedgerResult;
import org.nowstart.nyangnyangbot.application.port.in.favorite.usecase.AdjustFavoriteUseCase;
import org.nowstart.nyangnyangbot.application.port.in.upbo.dto.UpboApplyCommand;
import org.nowstart.nyangnyangbot.application.port.in.upbo.dto.UpboTemplateCreateCommand;
import org.nowstart.nyangnyangbot.application.port.out.upbo.dto.CreateUserUpboCommand;
import org.nowstart.nyangnyangbot.application.port.out.upbo.repository.UpboPort;
import org.nowstart.nyangnyangbot.domain.favorite.FavoriteSourceType;
import org.nowstart.nyangnyangbot.domain.model.UpboTemplate;
import org.nowstart.nyangnyangbot.domain.model.UserUpbo;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
import org.nowstart.nyangnyangbot.domain.type.UpboStatus;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class UpboService {

    private final UpboPort upboPort;
    private final AdjustFavoriteUseCase adjustFavoriteUseCase;

    public List<UpboTemplate> getActiveTemplates() {
        return upboPort.findActiveTemplates();
    }

    public UpboTemplate createTemplate(UpboTemplateCreateCommand command) {
        validateTemplateCommand(command);
        return upboPort.createTemplate(
                command.label().trim(),
                trimToEmpty(command.description()),
                command.displayOrder() == null ? 0 : command.displayOrder(),
                command.exchangeFavoriteValue(),
                command.rewardType(),
                command.conversionMode()
        );
    }

    public UserUpbo applyUpbo(UpboApplyCommand command, String actorId) {
        validateApplyCommand(command);
        UpboTemplate template = command.templateId() == null
                ? null
                : upboPort.findTemplateById(command.templateId())
                .orElseThrow(() -> new IllegalArgumentException("upbo template not found"));

        String label = template == null ? command.label().trim() : template.label();
        RewardType rewardType = template == null ? command.rewardType() : template.rewardType();
        ConversionMode conversionMode = template == null ? command.conversionMode() : template.conversionMode();
        Integer exchangeFavoriteValue = template == null ? command.exchangeFavoriteValue() : template.exchangeFavoriteValue();
        String publicDescription = command.publicDescription().trim();
        String privateMemo = command.privateMemo().trim();

        Long ledgerId = null;
        UpboStatus status = UpboStatus.OWNED;
        if (conversionMode == ConversionMode.AUTO) {
            if (exchangeFavoriteValue == null || exchangeFavoriteValue == 0) {
                throw new IllegalArgumentException("exchangeFavoriteValue is required for AUTO conversion");
            }
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
            status = UpboStatus.CONVERTED;
        }

        return upboPort.createUserUpbo(new CreateUserUpboCommand(
                command.userId(),
                template == null ? null : template.id(),
                trimToEmpty(command.nickName()),
                label,
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
    }

    public List<UserUpbo> getUserUpbos(String userId, UpboStatus status) {
        if (isBlank(userId)) {
            throw new IllegalArgumentException("userId is required");
        }
        return status == null
                ? upboPort.findUserUpbos(userId)
                : upboPort.findUserUpbosByStatus(userId, status);
    }

    private void validateTemplateCommand(UpboTemplateCreateCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("request is required");
        }
        if (isBlank(command.label())) {
            throw new IllegalArgumentException("label is required");
        }
        if (command.rewardType() == null) {
            throw new IllegalArgumentException("rewardType is required");
        }
        if (command.conversionMode() == null) {
            throw new IllegalArgumentException("conversionMode is required");
        }
        if (command.conversionMode() == ConversionMode.AUTO
                && (command.exchangeFavoriteValue() == null || command.exchangeFavoriteValue() == 0)) {
            throw new IllegalArgumentException("exchangeFavoriteValue is required for AUTO conversion");
        }
    }

    private void validateApplyCommand(UpboApplyCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("request is required");
        }
        if (isBlank(command.userId())) {
            throw new IllegalArgumentException("userId is required");
        }
        if (command.templateId() == null) {
            if (isBlank(command.label())) {
                throw new IllegalArgumentException("label is required");
            }
            if (command.rewardType() == null) {
                throw new IllegalArgumentException("rewardType is required");
            }
            if (command.conversionMode() == null) {
                throw new IllegalArgumentException("conversionMode is required");
            }
        }
        if (isBlank(command.publicDescription())) {
            throw new IllegalArgumentException("publicDescription is required");
        }
        if (isBlank(command.privateMemo())) {
            throw new IllegalArgumentException("privateMemo is required");
        }
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
