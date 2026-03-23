package org.nowstart.nyangnyangbot.service.command;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.data.dto.chzzk.ChatDto;
import org.nowstart.nyangnyangbot.data.dto.chzzk.MessageRequestDto;
import org.nowstart.nyangnyangbot.data.entity.FavoriteEntity;
import org.nowstart.nyangnyangbot.repository.ChzzkOpenApi;
import org.nowstart.nyangnyangbot.repository.FavoriteRepository;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class Favorite implements Command {

    private final ChzzkOpenApi chzzkOpenApi;
    private final FavoriteRepository favoriteRepository;

    @Override
    public void run(ChatDto chatDto) {
        FavoriteEntity favoriteEntity = favoriteRepository.findById(chatDto.senderChannelId())
                .orElseGet(() -> favoriteRepository.save(FavoriteEntity.builder()
                        .userId(chatDto.senderChannelId())
                        .nickName(chatDto.profile().nickname())
                        .totalFavorite(0)
                        .build()));

        int displayFavorite = resolveDisplayFavorite(favoriteEntity);
        log.info("[FAVORITE] : {}, {}", displayFavorite, chatDto);
        chzzkOpenApi.sendMessage(new MessageRequestDto(
                chatDto.profile().nickname() + "님의 호감도는 " + displayFavorite + " 입니다.💛"
        ));
    }

    private int resolveDisplayFavorite(FavoriteEntity favoriteEntity) {
        Integer totalFavorite = readIntegerProperty(favoriteEntity, "totalFavorite");
        if (totalFavorite != null) {
            return totalFavorite;
        }
        Integer legacyFavorite = readIntegerProperty(favoriteEntity, "favorite");
        return legacyFavorite != null ? legacyFavorite : 0;
    }

    private Integer readIntegerProperty(Object target, String propertyName) {
        BeanWrapperImpl beanWrapper = new BeanWrapperImpl(target);
        if (!beanWrapper.isReadableProperty(propertyName)) {
            return null;
        }
        Object value = beanWrapper.getPropertyValue(propertyName);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.valueOf(value.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
