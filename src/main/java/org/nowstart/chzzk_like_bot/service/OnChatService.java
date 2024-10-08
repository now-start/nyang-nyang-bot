package org.nowstart.chzzk_like_bot.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.chzzk_like_bot.entity.FavoriteEntity;
import org.nowstart.chzzk_like_bot.repository.FavoriteRepository;
import org.springframework.stereotype.Service;
import xyz.r2turntrue.chzzk4j.chat.ChatMessage;
import xyz.r2turntrue.chzzk4j.chat.ChzzkChat;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class OnChatService {

    private final FavoriteRepository favoriteRepository;

    public void onChat(ChzzkChat chat, ChatMessage msg) {
        switch (msg.getContent()) {
            case "!호감도":
                int favorite = getFavorite(msg.getUserId());
                log.info("[COMMAND][!호감도][{}][{}][{}]", msg.getUserId(), msg.getProfile().getNickname(), favorite);
                chat.sendChat("💛💛💛" + msg.getProfile().getNickname() + "님의 호감도는 " + favorite + " 입니다.💛💛💛");
                break;
            default:
                break;
        }
    }

    private int getFavorite(String userId) {
        return favoriteRepository.findByUserId(userId).orElse(new FavoriteEntity()).getFavorite();
    }
}
