package org.nowstart.nyangnyangbot.adapter.in.web.upbo;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.adapter.in.web.upbo.request.UpboApplyRequest;
import org.nowstart.nyangnyangbot.adapter.in.web.upbo.request.UpboTemplateCreateRequest;
import org.nowstart.nyangnyangbot.adapter.in.web.upbo.response.UpboApplyResponse;
import org.nowstart.nyangnyangbot.adapter.in.web.upbo.response.UpboTemplateResponse;
import org.nowstart.nyangnyangbot.application.port.in.upbo.ManageUpboUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/upbo")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Upbo API", description = "관리자 업보/쿠폰/리워드 처리 API")
public class AdminUpboController {

    private final ManageUpboUseCase manageUpboUseCase;

    @Operation(summary = "활성 업보 템플릿 조회")
    @GetMapping("/templates")
    public ResponseEntity<List<UpboTemplateResponse>> getTemplates() {
        return ResponseEntity.ok(manageUpboUseCase.getActiveTemplates().stream()
                .map(UpboTemplateResponse::from)
                .toList());
    }

    @Operation(summary = "업보 템플릿 생성")
    @PostMapping("/templates")
    public ResponseEntity<UpboTemplateResponse> createTemplate(
            @RequestBody UpboTemplateCreateRequest request
    ) {
        return ResponseEntity.ok(UpboTemplateResponse.from(manageUpboUseCase.createTemplate(request.toCreateTemplateCommand())));
    }

    @Operation(summary = "업보 수동 적용")
    @PostMapping("/apply")
    public ResponseEntity<UpboApplyResponse> applyUpbo(
            @RequestBody UpboApplyRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(UpboApplyResponse.from(
                manageUpboUseCase.applyUpbo(request.toApplyUpboCommand(), authentication.getName())
        ));
    }
}
