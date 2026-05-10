package org.nowstart.nyangnyangbot.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.data.dto.roulette.RouletteTableDto;
import org.nowstart.nyangnyangbot.service.RouletteService;
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

    private final RouletteService rouletteService;

    @Operation(summary = "룰렛 테이블 조회")
    @GetMapping("/tables")
    public ResponseEntity<List<RouletteTableDto.Response>> getTables() {
        return ResponseEntity.ok(rouletteService.getTables());
    }

    @Operation(summary = "룰렛 테이블 생성")
    @PostMapping("/tables")
    public ResponseEntity<RouletteTableDto.Response> createTable(
            @RequestBody RouletteTableDto.CreateRequest request
    ) {
        return ResponseEntity.ok(rouletteService.createTable(request));
    }

    @Operation(summary = "룰렛 항목 추가")
    @PostMapping("/tables/{tableId}/items")
    public ResponseEntity<RouletteTableDto.ItemResponse> addItem(
            @PathVariable Long tableId,
            @RequestBody RouletteTableDto.ItemRequest request
    ) {
        return ResponseEntity.ok(rouletteService.addItem(tableId, request));
    }

    @Operation(summary = "룰렛 활성화 검증")
    @GetMapping("/tables/{tableId}/validation")
    public ResponseEntity<RouletteTableDto.ValidationResponse> validateTable(@PathVariable Long tableId) {
        return ResponseEntity.ok(rouletteService.validateTable(tableId));
    }

    @Operation(summary = "룰렛 테이블 활성화")
    @PostMapping("/tables/{tableId}/activate")
    public ResponseEntity<RouletteTableDto.Response> activateTable(@PathVariable Long tableId) {
        return ResponseEntity.ok(rouletteService.activateTable(tableId));
    }

    @Operation(summary = "룰렛 테이블 비활성화")
    @PostMapping("/tables/{tableId}/deactivate")
    public ResponseEntity<RouletteTableDto.Response> deactivateTable(@PathVariable Long tableId) {
        return ResponseEntity.ok(rouletteService.deactivateTable(tableId));
    }

    @Operation(summary = "룰렛 확률 시뮬레이션")
    @GetMapping("/tables/{tableId}/simulation")
    public ResponseEntity<RouletteTableDto.SimulationResponse> simulate(
            @PathVariable Long tableId,
            @RequestParam(defaultValue = "10000") int iterations
    ) {
        return ResponseEntity.ok(rouletteService.simulate(tableId, iterations));
    }
}
