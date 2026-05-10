package org.nowstart.nyangnyangbot.adapter.out.external.chzzk;

import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort;
import org.nowstart.nyangnyangbot.application.dto.chzzk.ApiResponseDto;
import org.nowstart.nyangnyangbot.application.dto.chzzk.AuthorizationDto;
import org.nowstart.nyangnyangbot.application.dto.chzzk.AuthorizationRequestDto;
import org.nowstart.nyangnyangbot.application.dto.chzzk.MessageRequestDto;
import org.nowstart.nyangnyangbot.application.dto.chzzk.SessionDto;
import org.nowstart.nyangnyangbot.application.dto.chzzk.UserDto;
import org.nowstart.nyangnyangbot.adapter.out.external.chzzk.ChzzkOpenApi;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChzzkClientAdapter implements ChzzkClientPort {

    private final ChzzkOpenApi chzzkOpenApi;

    @Override
    public ApiResponseDto<AuthorizationDto> getAccessToken(AuthorizationRequestDto request) {
        return chzzkOpenApi.getAccessToken(request);
    }

    @Override
    public ApiResponseDto<UserDto> getUser(String authorization) {
        return chzzkOpenApi.getUser(authorization);
    }

    @Override
    public void sendMessage(MessageRequestDto request) {
        chzzkOpenApi.sendMessage(request);
    }

    @Override
    public void subscribeChatEvent(String sessionKey) {
        chzzkOpenApi.subscribeChatEvent(sessionKey);
    }

    @Override
    public void subscribeDonationEvent(String sessionKey) {
        chzzkOpenApi.subscribeDonationEvent(sessionKey);
    }

    @Override
    public void subscribeSubscriptionEvent(String sessionKey) {
        chzzkOpenApi.subscribeSubscriptionEvent(sessionKey);
    }

    @Override
    public ApiResponseDto<SessionDto> getSessionList(String clientId, String clientSecret) {
        return chzzkOpenApi.getSessionList(clientId, clientSecret);
    }

    @Override
    public ApiResponseDto<SessionDto> getSession(String clientId, String clientSecret) {
        return chzzkOpenApi.getSession(clientId, clientSecret);
    }
}
