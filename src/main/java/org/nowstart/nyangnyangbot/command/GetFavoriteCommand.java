package org.nowstart.nyangnyangbot.command;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.data.entity.FavoriteEntity;
import org.nowstart.nyangnyangbot.repository.FavoriteRepository;
import org.springframework.stereotype.Component;
import xyz.r2turntrue.chzzk4j.chat.ChatMessage;
import xyz.r2turntrue.chzzk4j.chat.ChzzkChat;


@Transactional
@RequiredArgsConstructor
//@Component("!호감도")
public class GetFavoriteCommand implements Command {

    private final FavoriteRepository favoriteRepository;

    @Override
    public void execute(ChzzkChat chat, ChatMessage msg) {
        int favorite = favoriteRepository.findByUserId(msg.getUserId()).orElse(new FavoriteEntity()).getFavorite();
        chat.sendChat( msg.getProfile().getNickname() + "님의 호감도는 " + favorite + " 입니다.💛");
    }
}