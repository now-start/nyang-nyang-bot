package org.nowstart.nyangnyangbot.repository;

import java.util.List;
import org.nowstart.nyangnyangbot.data.entity.UpboTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UpboTemplateRepository extends JpaRepository<UpboTemplateEntity, Long> {

    List<UpboTemplateEntity> findByActiveTrueOrderByDisplayOrderAscIdAsc();
}
