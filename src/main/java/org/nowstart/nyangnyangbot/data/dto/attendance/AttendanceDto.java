package org.nowstart.nyangnyangbot.data.dto.attendance;

import java.util.List;

public class AttendanceDto {

    public record ApplyRequest(List<User> users, Integer amount) {
    }

    public record ApplyResponse(Integer amount, Integer count) {
    }

    public record User(String userId, String nickName, Long lastMessageTime) {
    }
}
