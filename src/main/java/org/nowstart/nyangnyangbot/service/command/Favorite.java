package org.nowstart.nyangnyangbot.service.command;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.application.model.FavoriteSummary;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort;
import org.nowstart.nyangnyangbot.application.port.out.favorite.FavoriteQueryPort;
import org.nowstart.nyangnyangbot.data.dto.chzzk.ChatDto;
import org.nowstart.nyangnyangbot.data.dto.chzzk.MessageRequestDto;
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
