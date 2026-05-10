package org.nowstart.nyangnyangbot.application.command;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.domain.model.FavoriteSummary;
import org.nowstart.nyangnyangbot.application.gateway.out.chzzk.ChzzkClientPort;
import org.nowstart.nyangnyangbot.application.gateway.out.favorite.FavoriteQueryPort;
import org.nowstart.nyangnyangbot.application.chzzk.dto.ChatDto;
import org.nowstart.nyangnyangbot.application.chzzk.dto.MessageRequestDto;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class Favorite implements Command {

    private final ChzzkClientPort chzzkClientPort;
    private final FavoriteQueryPort favoriteQueryPort;

    @Override
    public void run(ChatDto chatDto) {
        FavoriteSummary favorite = favoriteQueryPort.getOrCreate(chatDto.senderChannelId(), chatDto.profile().nickname());

        log.info("[FAVORITE] : {}, {}", favorite.favorite(), chatDto);
        chzzkClientPort.sendMessage(new MessageRequestDto(
                chatDto.profile().nickname() + "님의 호감도는 " + favorite.favorite() + " 입니다.💛"
        ));
    }
}
