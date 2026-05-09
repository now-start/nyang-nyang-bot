package org.nowstart.nyangnyangbot.service;

import jakarta.transaction.Transactional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.application.favorite.AdjustFavoriteCommand;
import org.nowstart.nyangnyangbot.application.favorite.AdjustFavoriteUseCase;
import org.nowstart.nyangnyangbot.application.favorite.FavoriteLedgerResult;
import org.nowstart.nyangnyangbot.data.dto.upbo.UpboApplyDto;
import org.nowstart.nyangnyangbot.data.dto.upbo.UpboTemplateDto;
import org.nowstart.nyangnyangbot.data.dto.upbo.UserUpboDto;
import org.nowstart.nyangnyangbot.data.entity.UpboTemplateEntity;
import org.nowstart.nyangnyangbot.data.entity.UserUpboEntity;
import org.nowstart.nyangnyangbot.data.type.ConversionMode;
import org.nowstart.nyangnyangbot.data.type.RewardType;
import org.nowstart.nyangnyangbot.data.type.UpboStatus;
import org.nowstart.nyangnyangbot.domain.favorite.FavoriteSourceType;
import org.nowstart.nyangnyangbot.repository.UpboTemplateRepository;
import org.nowstart.nyangnyangbot.repository.UserUpboRepository;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class UpboService {

    private final UpboTemplateRepository upboTemplateRepository;
    private final UserUpboRepository userUpboRepository;
    private final AdjustFavoriteUseCase adjustFavoriteUseCase;

    public List<UpboTemplateDto.Response> getActiveTemplates() {
        return upboTemplateRepository.findByActiveTrueOrderByDisplayOrderAscIdAsc().stream()
                .map(UpboTemplateDto.Response::from)
                .toList();
    }

    public UpboTemplateDto.Response createTemplate(UpboTemplateDto.CreateRequest request) {
        validateTemplateRequest(request);
        UpboTemplateEntity saved = upboTemplateRepository.save(UpboTemplateEntity.builder()
                .label(request.label().trim())
                .description(trimToEmpty(request.description()))
                .active(true)
                .displayOrder(request.displayOrder() == null ? 0 : request.displayOrder())
                .exchangeFavoriteValue(request.exchangeFavoriteValue())
                .rewardType(request.rewardType())
                .conversionMode(request.conversionMode())
                .build());
        return UpboTemplateDto.Response.from(saved);
    }

    public UpboApplyDto.Response applyUpbo(UpboApplyDto.Request request, String actorId) {
        validateApplyRequest(request);
        UpboTemplateEntity template = request.templateId() == null
                ? null
                : upboTemplateRepository.findById(request.templateId())
                .orElseThrow(() -> new IllegalArgumentException("upbo template not found"));

        String label = template == null ? request.label().trim() : template.getLabel();
        RewardType rewardType = template == null ? request.rewardType() : template.getRewardType();
        ConversionMode conversionMode = template == null ? request.conversionMode() : template.getConversionMode();
        Integer exchangeFavoriteValue = template == null ? request.exchangeFavoriteValue() : template.getExchangeFavoriteValue();
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

        UserUpboEntity saved = userUpboRepository.save(UserUpboEntity.builder()
                .userId(request.userId())
                .upboTemplate(template)
                .nickNameSnapshot(trimToEmpty(request.nickName()))
                .label(label)
                .status(status)
                .exchangeFavoriteValue(exchangeFavoriteValue)
                .rewardType(rewardType)
                .conversionMode(conversionMode)
                .sourceType(FavoriteSourceType.UPBO_MANUAL)
                .ledgerId(ledgerId)
                .publicDescription(publicDescription)
                .privateMemo(privateMemo)
                .actorId(actorId)
                .build());
        return UpboApplyDto.Response.from(saved);
    }

    public List<UserUpboDto> getUserUpbos(String userId, UpboStatus status) {
        if (isBlank(userId)) {
            throw new IllegalArgumentException("userId is required");
        }
        List<UserUpboEntity> entities = status == null
                ? userUpboRepository.findByUserIdOrderByCreateDateDesc(userId)
                : userUpboRepository.findByUserIdAndStatusOrderByCreateDateDesc(userId, status);
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
