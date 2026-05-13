package org.nowstart.nyangnyangbot.adapter.in.web.favorite;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.adapter.in.web.favorite.request.FavoriteAdjustmentApplyRequest;
import org.nowstart.nyangnyangbot.adapter.in.web.favorite.request.FavoriteAdjustmentCreateRequest;
import org.nowstart.nyangnyangbot.adapter.in.web.favorite.response.FavoriteAdjustmentApplyResponse;
import org.nowstart.nyangnyangbot.adapter.in.web.favorite.response.FavoriteAdjustmentOptionResponse;
import org.nowstart.nyangnyangbot.application.port.in.favorite.ManageFavoriteAdjustmentUseCase;
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
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Favorite Adjustment API", description = "호감도 조정 항목 API")
public class FavoriteAdjustmentController {

    private final ManageFavoriteAdjustmentUseCase manageFavoriteAdjustmentUseCase;

    @Operation(summary = "호감도 조정 항목 조회")
    @GetMapping
    public ResponseEntity<List<FavoriteAdjustmentOptionResponse>> getAdjustments() {
        return ResponseEntity.ok(manageFavoriteAdjustmentUseCase.getAdjustments().stream()
                .map(FavoriteAdjustmentOptionResponse::from)
                .toList());
    }

    @Operation(summary = "호감도 조정 항목 추가")
    @PostMapping
    public ResponseEntity<FavoriteAdjustmentOptionResponse> createAdjustment(
            @RequestBody FavoriteAdjustmentCreateRequest request
    ) {
        return ResponseEntity.ok(FavoriteAdjustmentOptionResponse.from(
                manageFavoriteAdjustmentUseCase.createAdjustment(request.toCreateAdjustmentCommand())
        ));
    }

    @Operation(summary = "호감도 조정 적용")
    @PostMapping("/apply")
    public ResponseEntity<FavoriteAdjustmentApplyResponse> applyAdjustments(
            @RequestBody FavoriteAdjustmentApplyRequest request
    ) {
        return ResponseEntity.ok(FavoriteAdjustmentApplyResponse.from(
                manageFavoriteAdjustmentUseCase.applyAdjustments(request.toApplyAdjustmentCommand())
        ));
    }
}
