package org.nowstart.nyangnyangbot.adapter.in.web.roulette;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.adapter.in.web.roulette.request.RouletteItemRequest;
import org.nowstart.nyangnyangbot.adapter.in.web.roulette.request.RouletteTableCreateRequest;
import org.nowstart.nyangnyangbot.adapter.in.web.roulette.response.RouletteEventPageResponse;
import org.nowstart.nyangnyangbot.adapter.in.web.roulette.response.RouletteItemResponse;
import org.nowstart.nyangnyangbot.adapter.in.web.roulette.response.RouletteSimulationResponse;
import org.nowstart.nyangnyangbot.adapter.in.web.roulette.response.RouletteTableResponse;
import org.nowstart.nyangnyangbot.adapter.in.web.roulette.response.RouletteValidationResponse;
import org.nowstart.nyangnyangbot.application.port.in.roulette.ManageRouletteUseCase;
import org.nowstart.nyangnyangbot.application.port.in.roulette.QueryRouletteResultUseCase;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/roulette")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Roulette API", description = "관리자 후원 룰렛 API")
public class AdminRouletteController {

    private final ManageRouletteUseCase manageRouletteUseCase;
    private final QueryRouletteResultUseCase queryRouletteResultUseCase;

    @Operation(summary = "룰렛 테이블 조회")
    @GetMapping("/tables")
    public ResponseEntity<List<RouletteTableResponse>> getTables() {
        return ResponseEntity.ok(manageRouletteUseCase.getTables().stream()
                .map(RouletteTableResponse::from)
                .toList());
    }

    @Operation(summary = "최근 룰렛 실행 목록 조회")
    @GetMapping("/events")
    public ResponseEntity<RouletteEventPageResponse> getEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 20);
        Pageable pageable = PageRequest.of(safePage, safeSize);
        return ResponseEntity.ok(RouletteEventPageResponse.from(
                queryRouletteResultUseCase.getRecentEvents(pageable)
        ));
    }

    @Operation(summary = "룰렛 테이블 생성")
    @PostMapping("/tables")
    public ResponseEntity<RouletteTableResponse> createTable(
            @RequestBody RouletteTableCreateRequest request
    ) {
        return ResponseEntity.ok(RouletteTableResponse.from(
                manageRouletteUseCase.createTable(
                        request.title(),
                        request.command(),
                        request.pricePerRound(),
                        request.highRoundThreshold()
                )
        ));
    }

    @Operation(summary = "룰렛 항목 추가")
    @PostMapping("/tables/{tableId}/items")
    public ResponseEntity<RouletteItemResponse> addItem(
            @PathVariable Long tableId,
            @RequestBody RouletteItemRequest request
    ) {
        return ResponseEntity.ok(RouletteItemResponse.from(
                manageRouletteUseCase.addItem(
                        tableId,
                        request.label(),
                        request.probabilityBasisPoints(),
                        request.losingItem(),
                        request.rewardType(),
                        request.conversionMode(),
                        request.exchangeFavoriteValue(),
                        request.displayOrder()
                )
        ));
    }

    @Operation(summary = "룰렛 활성화 검증")
    @GetMapping("/tables/{tableId}/validation")
    public ResponseEntity<RouletteValidationResponse> validateTable(@PathVariable Long tableId) {
        return ResponseEntity.ok(RouletteValidationResponse.from(manageRouletteUseCase.validateTable(tableId)));
    }

    @Operation(summary = "룰렛 테이블 활성화")
    @PostMapping("/tables/{tableId}/activate")
    public ResponseEntity<RouletteTableResponse> activateTable(@PathVariable Long tableId) {
        return ResponseEntity.ok(RouletteTableResponse.from(manageRouletteUseCase.activateTable(tableId)));
    }

    @Operation(summary = "룰렛 테이블 비활성화")
    @PostMapping("/tables/{tableId}/deactivate")
    public ResponseEntity<RouletteTableResponse> deactivateTable(@PathVariable Long tableId) {
        return ResponseEntity.ok(RouletteTableResponse.from(manageRouletteUseCase.deactivateTable(tableId)));
    }

    @Operation(summary = "룰렛 확률 시뮬레이션")
    @GetMapping("/tables/{tableId}/simulation")
    public ResponseEntity<RouletteSimulationResponse> simulate(
            @PathVariable Long tableId,
            @RequestParam(defaultValue = "10000") int iterations
    ) {
        return ResponseEntity.ok(RouletteSimulationResponse.from(manageRouletteUseCase.simulate(tableId, iterations)));
    }
}
