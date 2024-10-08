package org.nowstart.chzzk_like_bot.service;

import jakarta.transaction.Transactional;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.chzzk_like_bot.dto.ResponseChannel.Content.Data.Channel;
import org.nowstart.chzzk_like_bot.entity.FavoriteEntity;
import org.nowstart.chzzk_like_bot.repository.ChzzkAPI;
import org.nowstart.chzzk_like_bot.repository.FavoriteRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final ChzzkAPI chzzkAPI;

    public Page<FavoriteEntity> getList(Pageable pageable) {
        return favoriteRepository.findAll(pageable);
    }

    public Page<FavoriteEntity> getByNickName(String nickName, Pageable pageable) {
        return favoriteRepository.findByNickNameContains(pageable, nickName);
    }

    public Optional<FavoriteEntity> getByUserId(String nickName) {
        return favoriteRepository.findByUserId(nickName);
    }

    public void addFavorite(String nickName, int favorite) {
        Channel channel = null;
        try {
            channel = chzzkAPI.getChannelId(nickName).getContent().getData().get(0).getChannel();
        } catch (Exception e) {
            throw new IllegalArgumentException("사용자 정보를 찾을 수 없습니다. [" + nickName + "]");
        }
        FavoriteEntity favoriteEntity = getByUserId(channel.getChannelId()).orElse(
            FavoriteEntity.builder()
                .userId(channel.getChannelId())
                .build());
        favoriteEntity.addFavorite(favorite);
        favoriteEntity.updateNickName(channel.getChannelName());
        favoriteRepository.save(favoriteEntity);
    }

    public void deleteFavorite(String userId) {
        favoriteRepository.deleteById(userId);
    }
}
