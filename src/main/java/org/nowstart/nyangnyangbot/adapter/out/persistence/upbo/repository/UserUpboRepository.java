package org.nowstart.nyangnyangbot.adapter.out.persistence.upbo.repository;

import org.nowstart.nyangnyangbot.adapter.out.persistence.upbo.entity.UserUpbo;

import java.util.List;
import org.nowstart.nyangnyangbot.domain.type.UpboStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserUpboRepository extends JpaRepository<UserUpbo, Long> {

    List<UserUpbo> findByUserIdOrderByCreateDateDesc(String userId);

    List<UserUpbo> findByUserIdAndStatusOrderByCreateDateDesc(String userId, UpboStatus status);
}
