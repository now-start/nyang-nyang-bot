package org.nowstart.chzzk_like_bot.repository;

import org.nowstart.chzzk_like_bot.entity.LikeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LikeRepository extends JpaRepository<LikeEntity, String> {
    LikeEntity findByUserId(String userId);

}
