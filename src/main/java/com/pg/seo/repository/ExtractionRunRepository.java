package com.pg.seo.repository;

import com.pg.seo.domain.ExtractionRun;
import com.pg.seo.domain.ExtractionStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExtractionRunRepository extends JpaRepository<ExtractionRun, Long> {

    List<ExtractionRun> findByOwnerUserIdOrderByStartedAtDesc(Long ownerUserId);

    Optional<ExtractionRun> findByIdAndOwnerUserId(Long id, Long ownerUserId);

    long countByOwnerUserIdAndStatus(Long ownerUserId, ExtractionStatus status);
}
