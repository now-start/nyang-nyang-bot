package org.nowstart.nyangnyangbot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.socket.emitter.Emitter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.data.dto.ChatDto;
import org.nowstart.nyangnyangbot.data.dto.MessageRequestDto;
import org.nowstart.nyangnyangbot.data.entity.AuthorizationEntity;
import org.nowstart.nyangnyangbot.data.entity.FavoriteEntity;
import org.nowstart.nyangnyangbot.repository.ChzzkOpenApi;
import org.nowstart.nyangnyangbot.repository.FavoriteRepository;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatService implements Emitter.Listener {

    private final AuthorizationService authorizationService;
    private final FavoriteRepository favoriteRepository;
    private final ChzzkOpenApi chzzkOpenApi;

    @Override
    @SneakyThrows
    public void call(Object... objects) {
        ObjectMapper objectMapper = new ObjectMapper();
        ChatDto chatDto = objectMapper.readValue((String) objects[0], ChatDto.class);

        if ("!호감도".equals(chatDto.getContent().split(" ")[0])) {
            AuthorizationEntity accessToken = authorizationService.getAccessToken();
            int favorite = favoriteRepository.findByUserId(chatDto.getSenderChannelId()).orElse(new FavoriteEntity()).getFavorite();

            chzzkOpenApi.sendMessage(accessToken.getTokenType() + " " + accessToken.getAccessToken(),
                MessageRequestDto.builder()
                    .message(chatDto.getProfile().getNickname() + "님의 호감도는 " + favorite + " 입니다.💛")
                    .build());
        }
    }
}