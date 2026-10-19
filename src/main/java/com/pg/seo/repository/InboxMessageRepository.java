package com.pg.seo.repository;

import com.pg.seo.domain.InboxMessage;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InboxMessageRepository extends JpaRepository<InboxMessage, Long> {

    List<InboxMessage> findByAccountIdAndReceivedAtBetweenOrderByReceivedAtDesc(
            Long accountId, Instant from, Instant to);

    Optional<InboxMessage> findByAccountIdAndProviderMessageId(Long accountId, String providerMessageId);

    boolean existsByAccountIdAndProviderMessageId(Long accountId, String providerMessageId);

    long countByAccountIdAndReceivedAtBetween(Long accountId, Instant from, Instant to);
}
