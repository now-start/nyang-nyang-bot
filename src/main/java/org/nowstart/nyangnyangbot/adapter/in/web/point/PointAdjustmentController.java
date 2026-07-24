package org.nowstart.nyangnyangbot.adapter.in.web.point;

import static org.nowstart.nyangnyangbot.application.port.in.point.ManagePointAdjustmentPresetUseCase.MAX_LABEL_LENGTH;
import static org.nowstart.nyangnyangbot.application.port.in.point.ManagePointAdjustmentPresetUseCase.MAX_MANUAL_DESCRIPTION_LENGTH;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.application.port.in.point.ManagePointAdjustmentPresetUseCase;
import org.nowstart.nyangnyangbot.application.port.in.point.ManagePointAdjustmentPresetUseCase.ApplyPointAdjustments;
import org.nowstart.nyangnyangbot.application.port.in.point.ManagePointAdjustmentPresetUseCase.CreatePointAdjustmentPreset;
import org.nowstart.nyangnyangbot.application.port.in.point.ManagePointAdjustmentPresetUseCase.PointAdjustmentPresetResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/points/adjustments")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Point Adjustment API", description = "호감도 포인트 조정 항목 API")
public class PointAdjustmentController {

    private static final String ADJUSTMENT_LIST_FRAGMENT = "features/point/overlays :: adjustment-list";
    private static final String MODAL_CONTENT_FRAGMENT = "features/point/overlays :: point-adjustment-modal-content";
    private static final String FEEDBACK_FRAGMENT = "components/feedback :: alert";
    private static final String APPLY_SUCCESS_TRIGGER = "{\"point-board-refresh\":{}}";

    private final ManagePointAdjustmentPresetUseCase managePointAdjustmentPresetUseCase;

    @Operation(summary = "호감도 조정 항목 조회")
    @GetMapping
    public String getAdjustments(Model model) {
        addPresets(model);
        return ADJUSTMENT_LIST_FRAGMENT;
    }

    @Operation(summary = "호감도 조정 모달 fragment 조회")
    @GetMapping("/modal")
    public String getAdjustmentModal(
            @ModelAttribute PointAdjustmentTarget target,
            Model model
    ) {
        addPresets(model);
        model.addAttribute("target", target);
        return MODAL_CONTENT_FRAGMENT;
    }

    @Operation(summary = "호감도 조정 항목 추가")
    @PostMapping
    public String createAdjustment(
            @Valid @ModelAttribute PointAdjustmentPresetCreateForm form,
            BindingResult bindingResult,
            Model model
    ) {
        if (!bindingResult.hasErrors()) {
            managePointAdjustmentPresetUseCase.createPreset(form.toCreatePreset());
        }
        addPresets(model);
        return ADJUSTMENT_LIST_FRAGMENT;
    }

    @Operation(summary = "호감도 조정 적용")
    @PostMapping("/apply")
    public String applyAdjustments(
            @Valid @ModelAttribute PointAdjustmentApplyForm form,
            BindingResult bindingResult,
            HttpServletResponse response,
            Authentication authentication,
        Model model
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("message", "포인트 조정 실패");
            model.addAttribute("tone", "danger");
            return FEEDBACK_FRAGMENT;
        }
        try {
            String actorUserId = authentication == null ? null : authentication.getName();
            managePointAdjustmentPresetUseCase.applyAdjustments(form.toApplyCommand(actorUserId));
            model.addAttribute("message", "포인트 조정 완료");
            model.addAttribute("tone", "success");
            response.addHeader("HX-Trigger", APPLY_SUCCESS_TRIGGER);
        } catch (RuntimeException e) {
            log.warn("Failed to apply point adjustments. userId={}", form.userId(), e);
            model.addAttribute("message", "포인트 조정 실패");
            model.addAttribute("tone", "danger");
        }
        return FEEDBACK_FRAGMENT;
    }

    private void addPresets(Model model) {
        List<PointAdjustmentPresetView> presets = managePointAdjustmentPresetUseCase.getPresets().stream()
                .map(PointAdjustmentPresetView::from)
                .toList();
        model.addAttribute("presets", presets);
    }

    public record PointAdjustmentTarget(
            String userId,
            String displayName,
            Long point
    ) {
    }

    public record PointAdjustmentPresetCreateForm(
            @NotNull(message = "amount is required")
            Long amount,
            @NotBlank(message = "label is required")
            @Size(max = MAX_LABEL_LENGTH, message = "label length must be 100 or less")
            String label
    ) {

        CreatePointAdjustmentPreset toCreatePreset() {
            return new CreatePointAdjustmentPreset(amount, label);
        }
    }

    public record PointAdjustmentApplyForm(
            @NotBlank(message = "userId is required")
            String userId,
            List<@Positive(message = "presetIds must be positive") Long> presetIds,
            Long manualAmount,
            @Size(max = MAX_MANUAL_DESCRIPTION_LENGTH,
                    message = "manualDescription length must be 500 or less")
            String manualDescription
    ) {

        @AssertTrue(message = "presetIds or manualAmount is required")
        public boolean hasAdjustment() {
            return (presetIds != null && !presetIds.isEmpty())
                    || (manualAmount != null && manualAmount != 0);
        }

        ApplyPointAdjustments toApplyCommand(String actorUserId) {
            return new ApplyPointAdjustments(userId, presetIds, manualAmount, manualDescription, actorUserId);
        }
    }

    public record PointAdjustmentPresetView(
            long id,
            long amount,
            String label
    ) {

        static PointAdjustmentPresetView from(PointAdjustmentPresetResult result) {
            return new PointAdjustmentPresetView(
                    result.id(),
                    result.amount(),
                    result.label()
            );
        }
    }
}
