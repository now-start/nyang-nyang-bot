package org.nowstart.nyangnyangbot.adapter.in.web.favorite;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.application.port.in.favorite.ManageFavoriteAdjustmentUseCase;
import org.nowstart.nyangnyangbot.application.port.in.favorite.ManageFavoriteAdjustmentUseCase.FavoriteAdjustmentApplyCommand;
import org.nowstart.nyangnyangbot.application.port.in.favorite.ManageFavoriteAdjustmentUseCase.FavoriteAdjustmentCreateCommand;
import org.nowstart.nyangnyangbot.application.port.in.favorite.ManageFavoriteAdjustmentUseCase.FavoriteAdjustmentOptionResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/favorite/adjustments")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Favorite Adjustment API", description = "호감도 조정 항목 API")
public class FavoriteAdjustmentController {

    private static final String ADJUSTMENT_LIST_FRAGMENT = "features/favorite/overlays :: adjustment-list";
    private static final String MODAL_CONTENT_FRAGMENT = "features/favorite/overlays :: karma-modal-content";
    private static final String FEEDBACK_FRAGMENT = "components/feedback :: alert";
    private static final String APPLY_SUCCESS_TRIGGER = "{\"favorite-board-refresh\":{}}";

    private final ManageFavoriteAdjustmentUseCase manageFavoriteAdjustmentUseCase;

    @Operation(summary = "호감도 조정 항목 조회")
    @GetMapping
    public String getAdjustments(Model model) {
        addAdjustments(model);
        return ADJUSTMENT_LIST_FRAGMENT;
    }

    @Operation(summary = "호감도 조정 모달 fragment 조회")
    @GetMapping("/modal")
    public String getAdjustmentModal(
            @ModelAttribute FavoriteAdjustmentTarget target,
            Model model
    ) {
        addAdjustments(model);
        model.addAttribute("target", target);
        return MODAL_CONTENT_FRAGMENT;
    }

    @Operation(summary = "호감도 조정 항목 추가")
    @PostMapping
    public String createAdjustment(
            @ModelAttribute FavoriteAdjustmentCreateForm form,
            Model model
    ) {
        manageFavoriteAdjustmentUseCase.createAdjustment(form.toCreateAdjustmentCommand());
        addAdjustments(model);
        return ADJUSTMENT_LIST_FRAGMENT;
    }

    @Operation(summary = "호감도 조정 적용")
    @PostMapping("/apply")
    public String applyAdjustments(
            @ModelAttribute FavoriteAdjustmentApplyForm form,
            HttpServletResponse response,
            Model model
    ) {
        try {
            manageFavoriteAdjustmentUseCase.applyAdjustments(form.toApplyAdjustmentCommand());
            model.addAttribute("message", "업보 적용 완료");
            model.addAttribute("tone", "success");
            response.addHeader("HX-Trigger", APPLY_SUCCESS_TRIGGER);
        } catch (RuntimeException e) {
            log.warn("Failed to apply favorite adjustments. userId={}", form.userId(), e);
            model.addAttribute("message", "업보 적용 실패");
            model.addAttribute("tone", "danger");
        }
        return FEEDBACK_FRAGMENT;
    }

    private void addAdjustments(Model model) {
        List<FavoriteAdjustmentOptionView> adjustments = manageFavoriteAdjustmentUseCase.getAdjustments().stream()
                .map(FavoriteAdjustmentOptionView::from)
                .toList();
        model.addAttribute("adjustments", adjustments);
    }

    public record FavoriteAdjustmentTarget(
            String userId,
            String nickName,
            Integer favorite
    ) {
    }

    public record FavoriteAdjustmentCreateForm(
            Integer amount,
            String label
    ) {

        FavoriteAdjustmentCreateCommand toCreateAdjustmentCommand() {
            return new FavoriteAdjustmentCreateCommand(amount, label);
        }
    }

    public record FavoriteAdjustmentApplyForm(
            String userId,
            List<Long> adjustmentIds,
            Integer manualAmount,
            String manualHistory
    ) {

        FavoriteAdjustmentApplyCommand toApplyAdjustmentCommand() {
            return new FavoriteAdjustmentApplyCommand(userId, adjustmentIds, manualAmount, manualHistory);
        }
    }

    public record FavoriteAdjustmentOptionView(
            Long id,
            Integer amount,
            String label
    ) {

        static FavoriteAdjustmentOptionView from(FavoriteAdjustmentOptionResult result) {
            return new FavoriteAdjustmentOptionView(
                    result.id(),
                    result.amount(),
                    result.label()
            );
        }
    }
}
