package org.nowstart.nyangnyangbot.data.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GoogleSheetDto {

    String nickName;
    String userId;
    int favorite;
}

