package org.nowstart.nyangnyangbot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.data.dto.FavoriteAdjustmentApplyRequest;
import org.nowstart.nyangnyangbot.data.dto.FavoriteAdjustmentApplyResponse;
import org.nowstart.nyangnyangbot.data.dto.FavoriteAdjustmentCreateRequest;
import org.nowstart.nyangnyangbot.data.dto.FavoriteAdjustmentResponse;
import org.nowstart.nyangnyangbot.service.FavoriteAdjustmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/favorite/adjustments")
@Tag(name = "Favorite Adjustment API", description = "호감도 조정 항목 API")
public class FavoriteAdjustmentController {

    private final FavoriteAdjustmentService favoriteAdjustmentService;

    @Operation(summary = "호감도 조정 항목 조회")
    @GetMapping
    public ResponseEntity<List<FavoriteAdjustmentResponse>> getAdjustments() {
        return ResponseEntity.ok(favoriteAdjustmentService.getAdjustments());
    }

    @Operation(summary = "호감도 조정 항목 추가")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<FavoriteAdjustmentResponse> createAdjustment(
            @RequestBody FavoriteAdjustmentCreateRequest request
    ) {
        return ResponseEntity.ok(favoriteAdjustmentService.createAdjustment(request));
    }

    @Operation(summary = "호감도 조정 적용")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/apply")
    public ResponseEntity<FavoriteAdjustmentApplyResponse> applyAdjustments(
            @RequestBody FavoriteAdjustmentApplyRequest request
    ) {
        return ResponseEntity.ok(
                favoriteAdjustmentService.applyAdjustments(
                        request.getUserId(),
                        request.getAdjustmentIds(),
                        request.getManualAmount(),
                        request.getManualHistory()
                )
        );
    }
}
