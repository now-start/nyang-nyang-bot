package org.nowstart.nyangnyangbot.application.service.command;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.application.port.out.favorite.FavoriteQueryPort.SummaryResult;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort;
import org.nowstart.nyangnyangbot.application.port.out.favorite.FavoriteQueryPort;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.ChatEventPayload;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.MessageCommand;
import org.springframework.stereotype.Service;

@Slf4j
@Service("favorite")
@Transactional
@RequiredArgsConstructor
public class Favorite implements CommandHandler {

    private final ChzzkClientPort chzzkClientPort;
    private final FavoriteQueryPort favoriteQueryPort;

    @Override
    public void run(ChatEventPayload chat) {
        SummaryResult favorite = favoriteQueryPort.getOrCreate(chat.senderChannelId(), chat.profile().nickname());

        log.info("[FAVORITE] : {}, {}", favorite.favorite(), chat);
        chzzkClientPort.sendMessage(new MessageCommand(
                chat.profile().nickname() + "님의 호감도는 " + favorite.favorite() + " 입니다.💛"
        ));
    }
}
