package org.nowstart.nyangnyangbot.adapter.in.web.command;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.adapter.in.web.command.request.CommandCreateRequest;
import org.nowstart.nyangnyangbot.adapter.in.web.command.request.CommandPreviewRequest;
import org.nowstart.nyangnyangbot.adapter.in.web.command.request.CommandUpdateRequest;
import org.nowstart.nyangnyangbot.adapter.in.web.command.request.CommandValidateRequest;
import org.nowstart.nyangnyangbot.adapter.in.web.command.response.CommandPreviewResponse;
import org.nowstart.nyangnyangbot.adapter.in.web.command.response.CommandResponse;
import org.nowstart.nyangnyangbot.adapter.in.web.command.response.CommandValidationResponse;
import org.nowstart.nyangnyangbot.application.port.in.command.ManageCommandUseCase;
import org.nowstart.nyangnyangbot.application.port.in.command.ManageCommandUseCase.CreateCommand;
import org.nowstart.nyangnyangbot.application.port.in.command.ManageCommandUseCase.PreviewCommand;
import org.nowstart.nyangnyangbot.application.port.in.command.ManageCommandUseCase.UpdateCommand;
import org.nowstart.nyangnyangbot.application.port.in.command.ManageCommandUseCase.ValidateCommand;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/commands")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Command API", description = "관리자 봇 명령어 API")
public class CommandController {

    private final ManageCommandUseCase manageCommandUseCase;

    @Operation(summary = "명령어 목록 조회")
    @GetMapping
    public ResponseEntity<List<CommandResponse>> getCommands() {
        return ResponseEntity.ok(manageCommandUseCase.getCommands().stream()
                .map(CommandResponse::from)
                .toList());
    }

    @Operation(summary = "명령어 생성")
    @PostMapping
    public ResponseEntity<CommandResponse> createCommand(
            @Valid @RequestBody CommandCreateRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(CommandResponse.from(manageCommandUseCase.createCommand(new CreateCommand(
                request.type(),
                request.trigger(),
                request.actionKey(),
                request.messageTemplate(),
                request.timerIntervalMinutes(),
                request.timerMinChatCount(),
                request.active(),
                request.requiredRole(),
                request.userCooldownSeconds(),
                actor(authentication)
        ))));
    }

    @Operation(summary = "명령어 수정")
    @PatchMapping("/{commandId}")
    public ResponseEntity<CommandResponse> updateCommand(
            @PathVariable Long commandId,
            @Valid @RequestBody CommandUpdateRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(CommandResponse.from(manageCommandUseCase.updateCommand(
                commandId,
                new UpdateCommand(
                        request.type(),
                        request.trigger(),
                        request.actionKey(),
                        request.messageTemplate(),
                        request.timerIntervalMinutes(),
                        request.timerMinChatCount(),
                        request.active(),
                        request.requiredRole(),
                        request.userCooldownSeconds(),
                        actor(authentication)
                )
        )));
    }

    @Operation(summary = "명령어 메시지 미리보기")
    @PostMapping("/preview")
    public ResponseEntity<CommandPreviewResponse> preview(
            @Valid @RequestBody CommandPreviewRequest request
    ) {
        return ResponseEntity.ok(CommandPreviewResponse.from(manageCommandUseCase.preview(new PreviewCommand(
                request.messageTemplate(),
                request.nickname(),
                request.command(),
                request.args(),
                request.arg1(),
                request.arg2(),
                request.favorite()
        ))));
    }

    @Operation(summary = "명령어 저장 전 검증")
    @PostMapping("/validate")
    public ResponseEntity<CommandValidationResponse> validate(
            @RequestBody CommandValidateRequest request
    ) {
        return ResponseEntity.ok(CommandValidationResponse.from(manageCommandUseCase.validate(new ValidateCommand(
                request.commandId(),
                request.type(),
                request.trigger(),
                request.actionKey(),
                request.messageTemplate(),
                request.timerIntervalMinutes(),
                request.timerMinChatCount(),
                request.requiredRole(),
                request.userCooldownSeconds()
        ))));
    }

    private String actor(Authentication authentication) {
        return authentication == null ? "system" : authentication.getName();
    }
}
