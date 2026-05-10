package org.nowstart.nyangnyangbot.adapter.out.persistence.repository;

import java.util.List;
import org.nowstart.nyangnyangbot.adapter.out.persistence.entity.OverlayTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OverlayTokenRepository extends JpaRepository<OverlayTokenEntity, Long> {

    List<OverlayTokenEntity> findByActiveTrue();

    boolean existsByTokenHashAndActiveTrue(String tokenHash);
}
