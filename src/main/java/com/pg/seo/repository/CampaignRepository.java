package com.pg.seo.repository;

import com.pg.seo.domain.Campaign;
import com.pg.seo.domain.CampaignStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampaignRepository extends JpaRepository<Campaign, Long> {

    List<Campaign> findByOwnerUserIdOrderByCreatedAtDesc(Long ownerUserId);

    List<Campaign> findByOwnerUserIdAndStatusOrderByCreatedAtDesc(Long ownerUserId, CampaignStatus status);

    Optional<Campaign> findByIdAndOwnerUserId(Long id, Long ownerUserId);

    long countByOwnerUserId(Long ownerUserId);

    long countByOwnerUserIdAndCreatedAtBetween(Long ownerUserId, Instant from, Instant to);

    long countByOwnerUserIdAndStatusAndCreatedAtBetween(
            Long ownerUserId, CampaignStatus status, Instant from, Instant to);
}
