package org.nowstart.nyangnyangbot.adapter.out.persistence.upbo.repository;

import java.util.List;
import org.nowstart.nyangnyangbot.adapter.out.persistence.upbo.entity.UpboTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
public interface UpboTemplateRepository extends JpaRepository<UpboTemplate, Long> {

    List<UpboTemplate> findByActiveTrueOrderByDisplayOrderAscIdAsc();
}
