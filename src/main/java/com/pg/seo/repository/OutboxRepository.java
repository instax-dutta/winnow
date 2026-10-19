package com.pg.seo.repository;

import com.pg.seo.domain.Outbox;
import com.pg.seo.domain.OutboxStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxRepository extends JpaRepository<Outbox, Long> {

    List<Outbox> findByStatusAndScheduledAtBeforeOrderByScheduledAtAsc(OutboxStatus status, Instant now);

    List<Outbox> findByOwnerUserIdAndScheduledAtBetweenOrderByScheduledAtDesc(
            Long ownerUserId, Instant from, Instant to);

    Optional<Outbox> findByIdAndOwnerUserId(Long id, Long ownerUserId);

    long countByOwnerUserIdAndStatus(Long ownerUserId, OutboxStatus status);

    /**
     * Claims one row by moving it out of the schedulable status.
     *
     * <p>The status is part of the WHERE clause, so two workers racing for the same row
     * produce one winner and one zero-row update. That is what makes the dispatch job
     * safe to run on more than one instance.
     */
    @Query("""
            select o from Outbox o
            where o.id = :id
              and o.status = :expected
              and o.deletedAt is null
            """)
    Optional<Outbox> findClaimable(@Param("id") Long id, @Param("expected") OutboxStatus expected);
}
