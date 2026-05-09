package org.nowstart.nyangnyangbot.repository;

import java.util.List;
import org.nowstart.nyangnyangbot.data.entity.UserUpboEntity;
import org.nowstart.nyangnyangbot.data.type.UpboStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserUpboRepository extends JpaRepository<UserUpboEntity, Long> {

    List<UserUpboEntity> findByUserIdOrderByCreateDateDesc(String userId);

    List<UserUpboEntity> findByUserIdAndStatusOrderByCreateDateDesc(String userId, UpboStatus status);
}
