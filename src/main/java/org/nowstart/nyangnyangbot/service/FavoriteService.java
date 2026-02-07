package org.nowstart.nyangnyangbot.service;

import jakarta.transaction.Transactional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.data.entity.FavoriteEntity;
import org.nowstart.nyangnyangbot.data.entity.FavoriteHistoryEntity;
import org.nowstart.nyangnyangbot.repository.FavoriteHistoryRepository;
import org.nowstart.nyangnyangbot.repository.FavoriteRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final FavoriteHistoryRepository favoriteHistoryRepository;

    public Page<FavoriteEntity> getList(Pageable pageable) {
        return favoriteRepository.findAll(pageable);
    }

    public Page<FavoriteEntity> getByNickName(Pageable pageable, String nickName) {
        return favoriteRepository.findByNickNameContains(pageable, nickName);
    }

    public List<FavoriteHistoryEntity> getHistory(String userId, int limit) {
        Pageable page = PageRequest.of(0, limit, Sort.by("modifyDate").descending());
        return favoriteHistoryRepository.findByFavoriteEntityUserId(userId, page).getContent();
    }
}
