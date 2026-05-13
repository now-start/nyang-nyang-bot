package org.nowstart.nyangnyangbot.adapter.out.external.chzzk.response;

import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.UserResult;

public record UserResponse(String channelId, String channelName, String status) {

    public UserResult toUserResult() {
        return new UserResult(channelId, channelName, status);
    }
}
