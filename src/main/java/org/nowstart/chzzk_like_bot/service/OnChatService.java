package org.nowstart.chzzk_like_bot.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.chzzk_like_bot.entity.FavoriteEntity;
import org.nowstart.chzzk_like_bot.entity.FavoriteHistoryEntity;
import org.nowstart.chzzk_like_bot.repository.FavoriteHistoryRepository;
import org.nowstart.chzzk_like_bot.repository.FavoriteRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import xyz.r2turntrue.chzzk4j.chat.ChatMessage;
import xyz.r2turntrue.chzzk4j.chat.ChzzkChat;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class OnChatService {

    @Value("${chzzk.channelId}")
    private String channelId;
    private final FavoriteRepository favoriteRepository;
    private final FavoriteHistoryRepository favoriteHistoryRepository;

    public void onChat(ChzzkChat chat, ChatMessage msg) {
        if ("!호감도".equals(msg.getContent())) {
            int favorite = getFavorite(msg.getUserId());
            log.info("[COMMAND][!호감도][{}][{}][{}]", msg.getUserId(), msg.getProfile().getNickname(), favorite);
            chat.sendChat("💛💛💛" + msg.getProfile().getNickname() + "님의 호감도는 " + favorite + " 입니다.💛💛💛");
        } else if (msg.getContent().startsWith("!호감도추가") && channelId.equals(msg.getUserId())) {
            try {
                String targetId = msg.getContent().split(" ")[1];
                int favorite = Integer.parseInt(msg.getContent().split(" ")[2]);
                FavoriteEntity favoriteEntity = favoriteRepository.findByNickName(targetId).orElseThrow(IllegalArgumentException::new);
                addFavorite(favoriteEntity.getUserId(), favoriteEntity.getNickName(), favorite, "채팅창에서 추가");
                chat.sendChat("💛💛💛" + favoriteEntity.getNickName() + "님의 호감도가 " + favorite + " 추가 되었어요.💛💛💛");
            } catch (Exception e) {
                chat.sendChat("호감도 추가를 실패 했어요.😓");
            }
        }
    }

    private int getFavorite(String userId) {
        return favoriteRepository.findByUserId(userId).orElse(new FavoriteEntity()).getFavorite();
    }

    public void addFavorite(String userId, String nickName, int favorite, String history) {
        FavoriteEntity favoriteEntity = favoriteRepository.findByUserId(userId).orElse(
            FavoriteEntity.builder()
                .userId(userId)
                .nickName(nickName)
                .build());
        favoriteEntity.addFavorite(favorite);
        FavoriteHistoryEntity favoriteHistoryEntity = FavoriteHistoryEntity.builder()
            .favoriteEntity(favoriteEntity)
            .favorite(favorite)
            .history(history)
            .build();
        favoriteHistoryRepository.save(favoriteHistoryEntity);
    }
}
