package org.nowstart.nyangnyangbot.adapter.in.web.favorite.response;

import org.nowstart.nyangnyangbot.application.port.in.weeklychat.QueryWeeklyChatRankUseCase.WeeklyChatRankView;

public record WeeklyChatRankResponse(
        Integer rank,
        String nickname,
        Long chatCount
) {

    public static WeeklyChatRankResponse from(WeeklyChatRankView view) {
        return new WeeklyChatRankResponse(view.rank(), view.nickname(), view.chatCount());
    }
}
