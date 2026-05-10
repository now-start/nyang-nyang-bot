package org.nowstart.nyangnyangbot.application.gateway.out.chzzk;

import org.nowstart.nyangnyangbot.application.chzzk.dto.ApiResponseDto;
import org.nowstart.nyangnyangbot.application.chzzk.dto.AuthorizationDto;
import org.nowstart.nyangnyangbot.application.chzzk.dto.AuthorizationRequestDto;
import org.nowstart.nyangnyangbot.application.chzzk.dto.MessageRequestDto;
import org.nowstart.nyangnyangbot.application.chzzk.dto.SessionDto;
import org.nowstart.nyangnyangbot.application.chzzk.dto.UserDto;

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
