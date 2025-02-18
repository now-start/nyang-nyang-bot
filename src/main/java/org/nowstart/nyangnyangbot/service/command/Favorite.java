package org.nowstart.nyangnyangbot.service.command;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.data.dto.ChatDto;
import org.nowstart.nyangnyangbot.data.dto.MessageRequestDto;
import org.nowstart.nyangnyangbot.data.entity.FavoriteEntity;
import org.nowstart.nyangnyangbot.repository.ChzzkOpenApi;
import org.nowstart.nyangnyangbot.repository.FavoriteRepository;
import org.springframework.stereotype.Service;
import xyz.r2turntrue.chzzk4j.chat.ChatMessage;
import xyz.r2turntrue.chzzk4j.chat.ChzzkChat;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class Favorite implements Command {

    private final ChzzkOpenApi chzzkOpenApi;
    private final FavoriteRepository favoriteRepository;

    @Override
    public void v1(ChzzkChat chat, ChatMessage msg) {
        int favorite = favoriteRepository.findById(msg.getUserId()).orElse(new FavoriteEntity()).getFavorite();
        chat.sendChat(msg.getProfile().getNickname() + "님의 호감도는 " + favorite + " 입니다.💛");
    }

    @Override
    public void v2(ChatDto chatDto) {
        int favorite = favoriteRepository.findById(chatDto.getSenderChannelId()).orElse(new FavoriteEntity()).getFavorite();
        chzzkOpenApi.sendMessage(MessageRequestDto.builder()
            .message(chatDto.getProfile().getNickname() + "님의 호감도는 " + favorite + " 입니다.💛")
            .build());
    }
}