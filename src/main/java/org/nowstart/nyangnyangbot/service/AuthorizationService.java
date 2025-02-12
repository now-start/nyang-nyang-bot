package org.nowstart.nyangnyangbot.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.data.dto.ChzzkDto;
import org.nowstart.nyangnyangbot.repository.AuthorizationRepository;
import org.nowstart.nyangnyangbot.repository.ChzzkOpenApi;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AuthorizationService {

    private final ChzzkOpenApi chzzkOpenApi;
    private final AuthorizationRepository authorizationRepository;

    public void getAccessToken(ChzzkDto chzzkDto, String code, String state) {
        chzzkOpenApi.getAccessToken("authorization_code", chzzkDto.getClientId(), chzzkDto.getClientSecret(), code, state);

    }

    public void getUser(String code) {
        chzzkOpenApi.getUser(code);
    }
}
