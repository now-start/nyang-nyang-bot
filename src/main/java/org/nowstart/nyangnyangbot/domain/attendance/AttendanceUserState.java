package org.nowstart.nyangnyangbot.domain.attendance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class AttendanceUserState {
    private final String userId;
    private String nickName;
    private long lastMessageTime;
}
