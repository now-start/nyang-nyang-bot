package org.nowstart.nyangnyangbot.adapter.out.persistence.upbo.repository;

import org.nowstart.nyangnyangbot.adapter.out.persistence.upbo.entity.UpboTemplate;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UpboTemplateRepository extends JpaRepository<UpboTemplate, Long> {

    List<UpboTemplate> findByActiveTrueOrderByDisplayOrderAscIdAsc();
}
