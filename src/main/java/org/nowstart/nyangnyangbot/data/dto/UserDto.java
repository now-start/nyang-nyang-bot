package org.nowstart.nyangnyangbot.data.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserDto {

    private String channelId;
    private String channelName;
}
