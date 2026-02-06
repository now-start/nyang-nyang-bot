package org.nowstart.nyangnyangbot.data.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceUserDto {

    private String userId;
    private String nickName;
    private Long lastMessageTime;
}
