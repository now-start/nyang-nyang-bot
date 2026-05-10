package org.nowstart.nyangnyangbot.application.model;

public record FavoriteSummary(
        String userId,
        String nickName,
        Integer favorite
) {
}
