package org.nowstart.nyangnyangbot.command;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.data.entity.FavoriteEntity;
import org.nowstart.nyangnyangbot.data.entity.FavoriteHistoryEntity;
import org.nowstart.nyangnyangbot.repository.FavoriteHistoryRepository;
import org.nowstart.nyangnyangbot.repository.FavoriteRepository;
import xyz.r2turntrue.chzzk4j.chat.ChatMessage;
import xyz.r2turntrue.chzzk4j.chat.ChzzkChat;

@Transactional
@RequiredArgsConstructor
//@Component("!호감도추가")
public class AddFavoriteCommand implements Command {

    private final FavoriteRepository favoriteRepository;
    private final FavoriteHistoryRepository favoriteHistoryRepository;

    @Override
    public void execute(ChzzkChat chat, ChatMessage msg) {
        if (chat.getChannelId().equals(msg.getUserId())) {
            try {
                String[] parts = msg.getContent().split(" ");
                String targetId = parts[1];
                int favorite = Integer.parseInt(parts[2]);

                FavoriteEntity favoriteEntity = favoriteRepository.findByNickName(targetId).orElseThrow(IllegalArgumentException::new);
                favoriteHistoryRepository.save(FavoriteHistoryEntity.builder()
                    .favoriteEntity(favoriteEntity.addFavorite(favorite))
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