package io.github.instaxdutta.winnow.repository;

import io.github.instaxdutta.winnow.domain.Campaign;
import io.github.instaxdutta.winnow.domain.CampaignStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CampaignRepository extends JpaRepository<Campaign, Long> {

    List<Campaign> findByOwnerUserIdOrderByCreatedAtDesc(Long ownerUserId);

    List<Campaign> findByOwnerUserIdAndStatusOrderByCreatedAtDesc(Long ownerUserId, CampaignStatus status);

    Optional<Campaign> findByIdAndOwnerUserId(Long id, Long ownerUserId);

    long countByOwnerUserId(Long ownerUserId);

    long countByOwnerUserIdAndCreatedAtBetween(Long ownerUserId, Instant from, Instant to);

    long countByOwnerUserIdAndStatusAndCreatedAtBetween(
            Long ownerUserId, CampaignStatus status, Instant from, Instant to);

    List<Campaign> findByStatusOrderByCreatedAtAsc(CampaignStatus status, org.springframework.data.domain.Pageable pageable);

    /**
     * Moves one campaign out of the queued status, in a single conditional statement.
     *
     * <p>The expected status is part of the WHERE clause, so two dispatchers racing for the
     * same campaign produce exactly one winner. The loser updates zero rows and does not
     * send the mail twice.
     *
     * @return 1 when this caller won the campaign, 0 when another dispatcher won it
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Campaign c
               set c.status = :running,
                   c.startedAt = :now,
                   c.updatedAt = :now
             where c.id = :id
               and c.status = :expected
            """)
    int claim(
            @Param("id") Long id,
            @Param("expected") CampaignStatus expected,
            @Param("running") CampaignStatus running,
            @Param("now") Instant now);
}
