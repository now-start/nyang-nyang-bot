package org.nowstart.nyangnyangbot.data.dto.attendance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class AttendanceUserDto {
    private final String userId;
    private String nickName;
    private long lastMessageTime;
}
