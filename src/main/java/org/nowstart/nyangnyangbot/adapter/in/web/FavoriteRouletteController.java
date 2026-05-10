package org.nowstart.nyangnyangbot.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.data.dto.roulette.RouletteRunDto;
import org.nowstart.nyangnyangbot.service.RouletteService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/favorite/roulette")
@Tag(name = "Favorite Roulette API", description = "사용자 룰렛 결과 조회 API")
public class FavoriteRouletteController {

    private final RouletteService rouletteService;

    @Operation(summary = "본인 룰렛 회차 결과 조회")
    @GetMapping("/results")
    public ResponseEntity<List<RouletteRunDto.RoundResponse>> getMyResults(
            Authentication authentication,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(rouletteService.getRecentRounds(authentication.getName(), limit));
    }
}
