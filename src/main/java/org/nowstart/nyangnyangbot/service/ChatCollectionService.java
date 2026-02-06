package org.nowstart.nyangnyangbot.service;

import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.data.dto.ChatDto;
import org.nowstart.nyangnyangbot.data.dto.CollectorStatusDto;
import org.nowstart.nyangnyangbot.data.entity.FavoriteEntity;
import org.nowstart.nyangnyangbot.repository.FavoriteRepository;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ChatCollectionService {

    private final FavoriteRepository favoriteRepository;
    private final AtomicBoolean collecting = new AtomicBoolean(false);
    private final Map<String, String> collectedNicknames = new ConcurrentHashMap<>();

    public void collect(ChatDto chatDto) {
        if (!collecting.get() || chatDto.getSenderChannelId() == null) {
            return;
        }

        String nickname = chatDto.getProfile() != null ? chatDto.getProfile().getNickname() : null;
        if (nickname == null) {
            nickname = "";
        }
        collectedNicknames.put(chatDto.getSenderChannelId(), nickname);
    }

    public CollectorStatusDto toggle() {
        if (collecting.get()) {
            collecting.set(false);
            List<String> collectedIds = new ArrayList<>(collectedNicknames.keySet());
            int updatedCount = applyFavorites(collectedIds);
            collectedNicknames.clear();
            return new CollectorStatusDto(false, 0, updatedCount);
        }

        collectedNicknames.clear();
        collecting.set(true);
        return new CollectorStatusDto(true, 0, 0);
    }

    public CollectorStatusDto status() {
        return new CollectorStatusDto(collecting.get(), collectedNicknames.size(), 0);
    }

    public List<String> getCollectedNicknames() {
        List<String> names = new ArrayList<>();
        for (Map.Entry<String, String> entry : collectedNicknames.entrySet()) {
            String nickname = entry.getValue();
            if (nickname == null || nickname.isEmpty()) {
                nickname = entry.getKey();
            }
            names.add(nickname);
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    private int applyFavorites(List<String> collectedIds) {
        int updatedCount = 0;
        for (String userId : collectedIds) {
            FavoriteEntity favoriteEntity = favoriteRepository.findById(userId)
                    .orElseGet(() -> favoriteRepository.save(FavoriteEntity.builder()
                            .userId(userId)
                            .nickName(collectedNicknames.getOrDefault(userId, ""))
                            .favorite(0)
                            .build()));
            Integer current = favoriteEntity.getFavorite();
            if (current == null) {
                current = 0;
            }
            favoriteEntity.setFavorite(current + 1);
            if (!collectedNicknames.getOrDefault(userId, "").isEmpty()) {
                favoriteEntity.setNickName(collectedNicknames.get(userId));
            }
            updatedCount += 1;
        }
        log.info("[COLLECTOR] updated favorites: {}", updatedCount);
        return updatedCount;
    }
}
