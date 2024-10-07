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
        log.debug("[Chat] {}: {}", msg.getProfile().getNickname(), msg.getContent());
        if (msg.getContent().contains("ㅋ") || true) {
            if (msg != null) {
                log.info("[테스트] msg 아이디 : {}", msg.getUserId());
                // 명령어 추가
                // chat.sendChat(msg.getProfile().getNickname() + "님의 호감도는 " + onChatService.getLike(msg.getUserId()) + " 입니다.");
            }
        }
    }

    private int getFavorite(String userId) {
        return favoriteRepository.findByUserId(userId).getFavorite();
    }

    private void save(FavoriteEntity favorite){
        favoriteRepository.save(favorite);
    }
}
