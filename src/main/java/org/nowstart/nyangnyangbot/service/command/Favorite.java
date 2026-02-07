package org.nowstart.nyangnyangbot.service.command;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.data.dto.chzzk.ChatDto;
import org.nowstart.nyangnyangbot.data.dto.chzzk.MessageRequestDto;
import org.nowstart.nyangnyangbot.data.entity.ChannelEntity;
import org.nowstart.nyangnyangbot.data.entity.FavoriteEntity;
import org.nowstart.nyangnyangbot.repository.ChzzkOpenApi;
import org.nowstart.nyangnyangbot.repository.FavoriteRepository;
import org.nowstart.nyangnyangbot.service.ChannelService;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class Favorite implements Command {

    private final ChzzkOpenApi chzzkOpenApi;
    private final FavoriteRepository favoriteRepository;
    private final ChannelService channelService;

    @Override
    public void run(ChatDto chatDto) {
        ChannelEntity ownerChannel = channelService.getDefaultChannel();
        ChannelEntity targetChannel = channelService.getOrCreate(chatDto.senderChannelId(), chatDto.profile().nickname());
        if (targetChannel == null) {
            return;
        }
        FavoriteEntity favoriteEntity = favoriteRepository
                .findByOwnerChannelIdAndTargetChannelId(ownerChannel.getId(), targetChannel.getId())
                .orElseGet(() -> favoriteRepository.save(FavoriteEntity.builder()
                        .ownerChannel(ownerChannel)
                        .targetChannel(targetChannel)
                        .favorite(0)
                        .build()));

        log.info("[FAVORITE] : {}, {}", favoriteEntity.getFavorite(), chatDto);
        chzzkOpenApi.sendMessage(new MessageRequestDto(
                chatDto.profile().nickname() + "?�의 ?�감?�는 " + favoriteEntity.getFavorite() + " ?�니???��"
        ));
    }
}
