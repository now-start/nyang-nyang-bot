package org.nowstart.nyangnyangbot.adapter.out.persistence.overlay.repository;

import java.util.List;
import org.nowstart.nyangnyangbot.adapter.out.persistence.overlay.entity.OverlayToken;
import org.springframework.data.jpa.repository.JpaRepository;
public interface OverlayTokenRepository extends JpaRepository<OverlayToken, Long> {

    List<OverlayToken> findByActiveTrue();

    boolean existsByTokenHashAndActiveTrue(String tokenHash);
}
