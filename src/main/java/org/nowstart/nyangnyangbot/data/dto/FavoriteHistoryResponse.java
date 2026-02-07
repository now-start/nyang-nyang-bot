package org.nowstart.nyangnyangbot.data.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FavoriteHistoryResponse {

    private Integer favorite;
    private String history;
    private String date;
}
