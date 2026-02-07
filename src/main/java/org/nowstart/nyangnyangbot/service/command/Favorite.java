package org.nowstart.nyangnyangbot.service.command;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.data.dto.chzzk.ChatDto;
import org.nowstart.nyangnyangbot.data.dto.chzzk.MessageRequestDto;
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
        FavoriteEntity favoriteEntity = favoriteRepository.findById(chatDto.senderChannelId())
                .orElseGet(() -> favoriteRepository.save(FavoriteEntity.builder()
                        .userId(chatDto.senderChannelId())
                        .nickName(chatDto.profile().nickname())
                        .favorite(0)
                        .build()));

        log.info("[FAVORITE] : {}, {}", favoriteEntity.getFavorite(), chatDto);
        chzzkOpenApi.sendMessage(new MessageRequestDto(
                chatDto.profile().nickname() + "?�의 ?�감?�는 " + favoriteEntity.getFavorite() + " ?�니???��"
        ));
    }
}
