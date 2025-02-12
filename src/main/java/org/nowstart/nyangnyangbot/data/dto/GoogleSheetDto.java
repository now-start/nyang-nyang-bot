package org.nowstart.nyangnyangbot.data.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GoogleSheetDto {

    private String nickName;
    private String userId;
    private int favorite;
}

