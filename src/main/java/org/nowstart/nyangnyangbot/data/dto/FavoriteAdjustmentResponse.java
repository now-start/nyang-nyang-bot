package org.nowstart.nyangnyangbot.data.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FavoriteAdjustmentResponse {

    private Long id;
    private Integer amount;
    private String label;
}
