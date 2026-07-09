package org.nowstart.nyangnyangbot.adapter.out.persistence.favorite.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.nowstart.nyangnyangbot.adapter.out.persistence.favorite.entity.FavoriteAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FavoriteRepository extends JpaRepository<FavoriteAccount, String> {

    Page<FavoriteAccount> findByNickNameContains(Pageable pageable, String nickName);

    long countByFavoriteGreaterThan(Integer favorite);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select favorite from FavoriteAccount favorite where favorite.userId = :userId")
    Optional<FavoriteAccount> findByIdForUpdate(@Param("userId") String userId);
}
