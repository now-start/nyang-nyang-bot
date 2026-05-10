package org.nowstart.nyangnyangbot.application.service;

import jakarta.transaction.Transactional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.application.favorite.AdjustFavoriteCommand;
import org.nowstart.nyangnyangbot.application.favorite.AdjustFavoriteUseCase;
import org.nowstart.nyangnyangbot.application.favorite.FavoriteLedgerResult;
import org.nowstart.nyangnyangbot.domain.model.UpboTemplate;
import org.nowstart.nyangnyangbot.domain.model.UserUpbo;
import org.nowstart.nyangnyangbot.application.gateway.out.upbo.CreateUserUpboCommand;
import org.nowstart.nyangnyangbot.application.gateway.out.upbo.UpboPort;
import org.nowstart.nyangnyangbot.application.dto.upbo.UpboApplyDto;
import org.nowstart.nyangnyangbot.application.dto.upbo.UpboTemplateDto;
import org.nowstart.nyangnyangbot.application.dto.upbo.UserUpboDto;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
import org.nowstart.nyangnyangbot.domain.type.UpboStatus;
import org.nowstart.nyangnyangbot.domain.favorite.FavoriteSourceType;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class UpboService {

    private final UpboPort upboPort;
    private final AdjustFavoriteUseCase adjustFavoriteUseCase;

    public List<UpboTemplateDto.Response> getActiveTemplates() {
        return upboPort.findActiveTemplates().stream()
                .map(UpboTemplateDto.Response::from)
                .toList();
    }

    public UpboTemplateDto.Response createTemplate(UpboTemplateDto.CreateRequest request) {
        validateTemplateRequest(request);
        UpboTemplate saved = upboPort.createTemplate(
                request.label().trim(),
                trimToEmpty(request.description()),
                request.displayOrder() == null ? 0 : request.displayOrder(),
                request.exchangeFavoriteValue(),
                request.rewardType(),
                request.conversionMode()
        );
        return UpboTemplateDto.Response.from(saved);
    }

    public UpboApplyDto.Response applyUpbo(UpboApplyDto.Request request, String actorId) {
        validateApplyRequest(request);
        UpboTemplate template = request.templateId() == null
                ? null
                : upboPort.findTemplateById(request.templateId())
                .orElseThrow(() -> new IllegalArgumentException("upbo template not found"));

        String label = template == null ? request.label().trim() : template.label();
        RewardType rewardType = template == null ? request.rewardType() : template.rewardType();
        ConversionMode conversionMode = template == null ? request.conversionMode() : template.conversionMode();
        Integer exchangeFavoriteValue = template == null ? request.exchangeFavoriteValue() : template.exchangeFavoriteValue();
        String publicDescription = request.publicDescription().trim();
        String privateMemo = request.privateMemo().trim();

        Long ledgerId = null;
        UpboStatus status = UpboStatus.OWNED;
        if (conversionMode == ConversionMode.AUTO) {
            if (exchangeFavoriteValue == null || exchangeFavoriteValue == 0) {
                throw new IllegalArgumentException("exchangeFavoriteValue is required for AUTO conversion");
            }
            FavoriteLedgerResult result = adjustFavoriteUseCase.adjust(AdjustFavoriteCommand.builder()
                    .userId(request.userId())
                    .nickName(request.nickName())
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

        UserUpbo saved = upboPort.createUserUpbo(new CreateUserUpboCommand(
                request.userId(),
                template == null ? null : template.id(),
                trimToEmpty(request.nickName()),
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
        return UpboApplyDto.Response.from(saved);
    }

    public List<UserUpboDto> getUserUpbos(String userId, UpboStatus status) {
        if (isBlank(userId)) {
            throw new IllegalArgumentException("userId is required");
        }
        List<UserUpbo> entities = status == null
                ? upboPort.findUserUpbos(userId)
                : upboPort.findUserUpbosByStatus(userId, status);
        return entities.stream()
                .map(UserUpboDto::from)
                .toList();
    }

    private void validateTemplateRequest(UpboTemplateDto.CreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        if (isBlank(request.label())) {
            throw new IllegalArgumentException("label is required");
        }
        if (request.rewardType() == null) {
            throw new IllegalArgumentException("rewardType is required");
        }
        if (request.conversionMode() == null) {
            throw new IllegalArgumentException("conversionMode is required");
        }
        if (request.conversionMode() == ConversionMode.AUTO
                && (request.exchangeFavoriteValue() == null || request.exchangeFavoriteValue() == 0)) {
            throw new IllegalArgumentException("exchangeFavoriteValue is required for AUTO conversion");
        }
    }

    private void validateApplyRequest(UpboApplyDto.Request request) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        if (isBlank(request.userId())) {
            throw new IllegalArgumentException("userId is required");
        }
        if (request.templateId() == null) {
            if (isBlank(request.label())) {
                throw new IllegalArgumentException("label is required");
            }
            if (request.rewardType() == null) {
                throw new IllegalArgumentException("rewardType is required");
            }
            if (request.conversionMode() == null) {
                throw new IllegalArgumentException("conversionMode is required");
            }
        }
        if (isBlank(request.publicDescription())) {
            throw new IllegalArgumentException("publicDescription is required");
        }
        if (isBlank(request.privateMemo())) {
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
