package org.nowstart.nyangnyangbot.adapter.in.web.upbo;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.adapter.in.web.upbo.response.UserUpboResponse;
import org.nowstart.nyangnyangbot.application.service.upbo.UpboService;
import org.nowstart.nyangnyangbot.domain.type.UpboStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/favorite/upbo")
@Tag(name = "Favorite Upbo API", description = "사용자 업보/쿠폰/리워드 조회 API")
public class FavoriteUpboController {

    private final UpboService upboService;

    @Operation(summary = "사용자 업보 내역 조회", description = "ADMIN은 전체 조회, 그 외는 본인 계정만 조회 가능합니다.")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.name")
    public ResponseEntity<List<UserUpboResponse>> getUserUpbos(
            @RequestParam String userId,
            @RequestParam(required = false) UpboStatus status
    ) {
        return ResponseEntity.ok(upboService.getUserUpbos(userId, status).stream()
                .map(UserUpboResponse::from)
                .toList());
    }
}
