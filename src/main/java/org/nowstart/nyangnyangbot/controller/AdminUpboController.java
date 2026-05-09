package org.nowstart.nyangnyangbot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.data.dto.upbo.UpboApplyDto;
import org.nowstart.nyangnyangbot.data.dto.upbo.UpboTemplateDto;
import org.nowstart.nyangnyangbot.service.UpboService;
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

    private final UpboService upboService;

    @Operation(summary = "활성 업보 템플릿 조회")
    @GetMapping("/templates")
    public ResponseEntity<List<UpboTemplateDto.Response>> getTemplates() {
        return ResponseEntity.ok(upboService.getActiveTemplates());
    }

    @Operation(summary = "업보 템플릿 생성")
    @PostMapping("/templates")
    public ResponseEntity<UpboTemplateDto.Response> createTemplate(
            @RequestBody UpboTemplateDto.CreateRequest request
    ) {
        return ResponseEntity.ok(upboService.createTemplate(request));
    }

    @Operation(summary = "업보 수동 적용")
    @PostMapping("/apply")
    public ResponseEntity<UpboApplyDto.Response> applyUpbo(
            @RequestBody UpboApplyDto.Request request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(upboService.applyUpbo(request, authentication.getName()));
    }
}
