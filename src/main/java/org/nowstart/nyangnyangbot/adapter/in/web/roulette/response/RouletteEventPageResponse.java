package org.nowstart.nyangnyangbot.adapter.in.web.roulette.response;

import java.util.List;
import org.nowstart.nyangnyangbot.application.port.in.roulette.QueryRouletteResultUseCase.RouletteEventSummaryResult;
import org.springframework.data.domain.Page;

public record RouletteEventPageResponse(
        List<RouletteEventSummaryResponse> content,
        int number,
        int size,
        long totalElements,
        int totalPages,
        int numberOfElements,
        boolean first,
        boolean last,
        boolean empty
) {

    public static RouletteEventPageResponse from(Page<RouletteEventSummaryResult> page) {
        return new RouletteEventPageResponse(
                page.getContent().stream()
                        .map(RouletteEventSummaryResponse::from)
                        .toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumberOfElements(),
                page.isFirst(),
                page.isLast(),
                page.isEmpty()
        );
    }
}
