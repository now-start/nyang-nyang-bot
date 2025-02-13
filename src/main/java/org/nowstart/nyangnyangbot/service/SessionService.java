package org.nowstart.nyangnyangbot.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.data.entity.AuthorizationEntity;
import org.nowstart.nyangnyangbot.repository.ChzzkOpenApi;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SessionService {

    private final AuthorizationService authorizationService;
    private final ChzzkOpenApi chzzkOpenApi;

    public String getSession() {
        AuthorizationEntity accessToken = authorizationService.getAccessToken();
        return chzzkOpenApi.getSession(accessToken.getTokenType() + " " + accessToken.getAccessToken()).getContent().getUrl();
    }

    public void connectChat(String sessionKey) {
        AuthorizationEntity accessToken = authorizationService.getAccessToken();
        chzzkOpenApi.subscribeChatEvent(accessToken.getTokenType() + " " + accessToken.getAccessToken(), sessionKey);
    }
}
