package org.nowstart.nyangnyangbot.application.gateway.out.chzzk;

import org.nowstart.nyangnyangbot.application.dto.chzzk.ApiResponseDto;
import org.nowstart.nyangnyangbot.application.dto.chzzk.AuthorizationDto;
import org.nowstart.nyangnyangbot.application.dto.chzzk.AuthorizationRequestDto;
import org.nowstart.nyangnyangbot.application.dto.chzzk.MessageRequestDto;
import org.nowstart.nyangnyangbot.application.dto.chzzk.SessionDto;
import org.nowstart.nyangnyangbot.application.dto.chzzk.UserDto;

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
