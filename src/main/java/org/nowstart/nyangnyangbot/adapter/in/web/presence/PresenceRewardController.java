package org.nowstart.nyangnyangbot.adapter.in.web.presence;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.application.port.in.presence.ManagePresenceRewardUseCase;
import org.nowstart.nyangnyangbot.application.port.in.presence.ManagePresenceRewardUseCase.PresenceApplyCommand;
import org.nowstart.nyangnyangbot.application.port.in.presence.ManagePresenceRewardUseCase.PresenceUserSnapshot;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/presence-rewards")
@Tag(name = "Presence Reward API", description = "생존자 수집 및 포인트 지급 API")
@PreAuthorize("hasRole('ADMIN')")
public class PresenceRewardController {

    private static final String PRESENCE_LIST_FRAGMENT = "features/presence/components :: presence-list";
    private static final String FEEDBACK_FRAGMENT = "features/presence/components :: presence-feedback-response";
    private static final String USERS_REFRESH_TRIGGER = "presence-users-refresh";
    private static final String APPLY_SUCCESS_TRIGGER = "{\"point-board-refresh\":{}}";

    private final ManagePresenceRewardUseCase managePresenceRewardUseCase;

    @Operation(summary = "현재 채팅 사용자 목록 조회")
    @RequestMapping(path = "/users", method = {RequestMethod.GET, RequestMethod.POST})
    public String getUsers(
            @RequestParam(required = false) List<String> userIds,
            @RequestParam(required = false) List<String> knownUserIds,
            @RequestParam(defaultValue = "false") boolean selectionInitialized,
            Model model
    ) {
        addUsers(model, userIds, knownUserIds, selectionInitialized);
        return PRESENCE_LIST_FRAGMENT;
    }

    @Operation(summary = "출석체크 수집 시작")
    @PostMapping("/start")
    public String startCapture(HttpServletResponse response, Model model) {
        managePresenceRewardUseCase.startCapture();
        model.addAttribute("message", "생존자 수집 시작");
        model.addAttribute("tone", "success");
        model.addAttribute("resetPresenceList", false);
        response.addHeader("HX-Trigger", USERS_REFRESH_TRIGGER);
        return FEEDBACK_FRAGMENT;
    }

    @Operation(summary = "출석체크 수집 종료")
    @PostMapping("/stop")
    public String stopCapture(Model model) {
        managePresenceRewardUseCase.stopCapture();
        model.addAttribute("message", "생존자 수집 종료");
        model.addAttribute("tone", "secondary");
        model.addAttribute("resetPresenceList", true);
        return FEEDBACK_FRAGMENT;
    }

    @Operation(summary = "생존자 포인트 지급")
    @PostMapping("/apply")
    public String applyPresenceReward(
            @Valid @ModelAttribute PresenceRewardApplyForm form,
            BindingResult bindingResult,
            HttpServletResponse response,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("message", "생존자 보상 실패");
            model.addAttribute("tone", "danger");
            model.addAttribute("resetPresenceList", false);
            return FEEDBACK_FRAGMENT;
        }
        try {
            managePresenceRewardUseCase.applyPresenceReward(new PresenceApplyCommand(form.userIds(), form.amount()));
            model.addAttribute("message", "생존 확인 보상 완료");
            model.addAttribute("tone", "success");
            model.addAttribute("resetPresenceList", true);
            response.addHeader("HX-Trigger", APPLY_SUCCESS_TRIGGER);
        } catch (RuntimeException e) {
            log.warn("Failed to apply presence reward.", e);
            model.addAttribute("message", "생존자 보상 실패");
            model.addAttribute("tone", "danger");
            model.addAttribute("resetPresenceList", false);
        }
        return FEEDBACK_FRAGMENT;
    }

    private void addUsers(
            Model model,
            List<String> selectedUserIds,
            List<String> knownUserIds,
            boolean selectionInitialized
    ) {
        List<PresenceUserView> users = managePresenceRewardUseCase.getActiveUsers().stream()
                .map(PresenceUserView::from)
                .toList();
        Set<String> selectedIds = selectionInitialized
                ? (selectedUserIds == null ? Set.of() : new HashSet<>(selectedUserIds))
                : null;
        Set<String> knownIds = selectionInitialized
                ? (knownUserIds == null ? Set.of() : new HashSet<>(knownUserIds))
                : Set.of();
        long selectedCount = selectedIds == null
                ? users.size()
                : users.stream()
                .filter(user -> selectedIds.contains(user.userId()) || !knownIds.contains(user.userId()))
                .count();
        model.addAttribute("users", users);
        model.addAttribute("selectedUserIds", selectedIds);
        model.addAttribute("knownUserIds", knownIds);
        model.addAttribute("selectedCount", selectedCount);
        model.addAttribute("totalCount", users.size());
    }

    public record PresenceRewardApplyForm(
            @NotNull(message = "amount is required")
            @Positive(message = "amount must be positive")
            Long amount,
            @NotEmpty(message = "presence targets are required")
            List<@NotBlank(message = "userId is required") String> userIds
    ) {
    }

    public record PresenceUserView(
            String userId,
            String displayName,
            Long lastMessageTime
    ) {

        static PresenceUserView from(PresenceUserSnapshot snapshot) {
            return new PresenceUserView(snapshot.userId(), snapshot.displayName(), snapshot.lastMessageTime());
        }
    }
}
