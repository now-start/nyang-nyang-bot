package org.nowstart.nyangnyangbot.application.port.out.chzzk;

import org.nowstart.nyangnyangbot.data.dto.chzzk.ApiResponseDto;
import org.nowstart.nyangnyangbot.data.dto.chzzk.AuthorizationDto;
import org.nowstart.nyangnyangbot.data.dto.chzzk.AuthorizationRequestDto;
import org.nowstart.nyangnyangbot.data.dto.chzzk.MessageRequestDto;
import org.nowstart.nyangnyangbot.data.dto.chzzk.SessionDto;
import org.nowstart.nyangnyangbot.data.dto.chzzk.UserDto;

public interface ChzzkClientPort {

    ApiResponseDto<AuthorizationDto> getAccessToken(AuthorizationRequestDto request);

    ApiResponseDto<UserDto> getUser(String authorization);

    void sendMessage(MessageRequestDto request);

    void subscribeChatEvent(String sessionKey);

    void subscribeDonationEvent(String sessionKey);

    void subscribeSubscriptionEvent(String sessionKey);

    ApiResponseDto<SessionDto> getSessionList(String clientId, String clientSecret);

    ApiResponseDto<SessionDto> getSession(String clientId, String clientSecret);
}
