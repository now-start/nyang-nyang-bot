package org.nowstart.nyangnyangbot.adapter.out.persistence.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.nowstart.nyangnyangbot.adapter.out.persistence.entity.FavoriteEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FavoriteRepository extends JpaRepository<FavoriteEntity, String> {

    Page<FavoriteEntity> findByNickNameContains(Pageable pageable, String nickName);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select favorite from FavoriteEntity favorite where favorite.userId = :userId")
    Optional<FavoriteEntity> findByIdForUpdate(@Param("userId") String userId);
}
