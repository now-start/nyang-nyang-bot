package org.nowstart.chzzk_like_bot.command;

import lombok.RequiredArgsConstructor;
import org.nowstart.chzzk_like_bot.data.entity.FavoriteEntity;
import org.nowstart.chzzk_like_bot.data.entity.FavoriteHistoryEntity;
import org.nowstart.chzzk_like_bot.repository.FavoriteHistoryRepository;
import org.nowstart.chzzk_like_bot.repository.FavoriteRepository;
import org.springframework.beans.factory.annotation.Value;
import xyz.r2turntrue.chzzk4j.chat.ChatMessage;
import xyz.r2turntrue.chzzk4j.chat.ChzzkChat;


@RequiredArgsConstructor
//@Component("!호감도추가")
public class AddFavoriteCommand implements Command {

    private final FavoriteRepository favoriteRepository;
    private final FavoriteHistoryRepository favoriteHistoryRepository;
    @Value("${chzzk.channelId}")
    private String channelId;

    @Override
    public void execute(ChzzkChat chat, ChatMessage msg) {
        if (channelId.equals(msg.getUserId())) {
            try {
                String[] parts = msg.getContent().split(" ");
                String targetId = parts[1];
                int favorite = Integer.parseInt(parts[2]);
                FavoriteEntity favoriteEntity = favoriteRepository.findByNickName(targetId).orElseThrow(IllegalArgumentException::new);
                favoriteEntity.addFavorite(favorite);
                favoriteRepository.save(favoriteEntity);
                favoriteHistoryRepository.save(FavoriteHistoryEntity.builder()
                    .favoriteEntity(favoriteEntity)
                    .favorite(favorite)
                    .history("채팅창에서 추가")
                    .build());
                chat.sendChat("💛💛💛" + favoriteEntity.getNickName() + "님의 호감도가 " + favorite + " 추가 되었어요.💛💛💛");
            } catch (Exception e) {
                chat.sendChat("호감도 추가를 실패 했어요.😓");
            }
        }
    }
}