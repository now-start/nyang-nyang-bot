package org.nowstart.nyangnyangbot.adapter.out.persistence.overlay.repository;

import org.nowstart.nyangnyangbot.adapter.out.persistence.overlay.entity.OverlayTokenEntity;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OverlayTokenRepository extends JpaRepository<OverlayTokenEntity, Long> {

    List<OverlayTokenEntity> findByActiveTrue();

    boolean existsByTokenHashAndActiveTrue(String tokenHash);
}
