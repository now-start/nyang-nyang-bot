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

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class Favorite implements Command {

    private final ChzzkOpenApi chzzkOpenApi;
    private final FavoriteRepository favoriteRepository;

    @Override
    public void run(ChatDto chatDto) {
        FavoriteEntity favoriteEntity = favoriteRepository.findById(chatDto.getSenderChannelId())
            .orElseGet(() -> {
                FavoriteEntity newEntity = FavoriteEntity.builder()
                    .userId(chatDto.getSenderChannelId())
                    .nickName(chatDto.getProfile().getNickname())
                    .favorite(0)
                    .build();
                return favoriteRepository.save(newEntity);
            });

        log.info("[FAVORITE] : {}, {}", favoriteEntity.getFavorite(), chatDto);
        chzzkOpenApi.sendMessage(MessageRequestDto.builder()
            .message(chatDto.getProfile().getNickname() + "님의 호감도는 " + favoriteEntity.getFavorite() + " 입니다.💛")
            .build());
    }
}