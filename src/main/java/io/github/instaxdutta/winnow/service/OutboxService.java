package io.github.instaxdutta.winnow.service;

import io.github.instaxdutta.winnow.domain.ConflictException;
import io.github.instaxdutta.winnow.domain.NotFoundException;
import io.github.instaxdutta.winnow.domain.Outbox;
import io.github.instaxdutta.winnow.domain.OutboxStatus;
import io.github.instaxdutta.winnow.model.OutboxDto;
import io.github.instaxdutta.winnow.model.OutboxRequest;
import io.github.instaxdutta.winnow.repository.OutboxRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The scheduled outbox and the dispatch that drains it.
 *
 * <p>A row is claimed with a conditional update, so two workers that race for the same row
 * produce one winner and the loser moves on. A row is therefore never sent twice.
 */
@Service
public class OutboxService {

    private final OutboxRepository outboxes;

    /**
     * The conditional claim is a single statement, so it needs the entity manager. It
     * belongs in the repository next to {@code findClaimable}; the repository does not
     * expose a conditional update yet.
     */
    public OutboxService(OutboxRepository outboxes) {
        this.outboxes = outboxes;
    }

    @Transactional
    public OutboxDto schedule(Long ownerUserId, OutboxRequest request) {
        Instant now = Instant.now();
        Outbox row = new Outbox();
        row.setOwnerUserId(ownerUserId);
        row.setFromAccountId(request.fromAccountId());
        row.setToEmail(request.toEmail().trim());
        row.setSubject(request.subject());
        row.setBodyHtml(request.bodyHtml());
        row.setScheduledAt(request.scheduledAt());
        row.setStatus(OutboxStatus.SCHEDULED);
        row.setAttemptCount(0);
        row.setCreatedAt(now);
        row.setCreatedBy(ownerUserId);
        row.setUpdatedAt(now);
        row.setUpdatedBy(ownerUserId);
        outboxes.save(row);
        return toDto(row);
    }

    /**
     * One outbox row of the tenant.
     *
     * @param ownerUserId the tenant
     * @param id          the row
     * @return the row
     * @throws NotFoundException when the row does not exist or belongs to another tenant
     */
    @Transactional(readOnly = true)
    public OutboxDto get(Long ownerUserId, Long id) {
        Outbox row = outboxes.findByIdAndOwnerUserId(id, ownerUserId)
                .orElseThrow(() -> NotFoundException.of("Outbox row", id));
        return toDto(row);
    }

    @Transactional(readOnly = true)
    public List<OutboxDto> list(Long ownerUserId, Instant from, Instant to) {
        return outboxes.findByOwnerUserIdAndScheduledAtBetweenOrderByScheduledAtDesc(ownerUserId, from, to).stream()
                .map(OutboxService::toDto)
                .toList();
    }

    @Transactional
    public void cancel(Long ownerUserId, Long id) {
        Outbox row = outboxes
                .findByIdAndOwnerUserId(id, ownerUserId)
                .orElseThrow(() -> NotFoundException.of("Outbox row", id));
        if (row.getStatus() == OutboxStatus.SENT || row.getStatus() == OutboxStatus.CANCELLED) {
            throw new ConflictException("An outbox row in the state " + row.getStatus() + " cannot be cancelled");
        }
        Instant now = Instant.now();
        row.setStatus(OutboxStatus.CANCELLED);
        row.setUpdatedAt(now);
        row.setUpdatedBy(ownerUserId);
        outboxes.save(row);
    }

    /**
     * Claims the rows that are due.
     *
     * <p>The claim is a conditional update: the status is part of the WHERE clause, so only
     * one worker moves a row out of {@code SCHEDULED}. The rows that were won are returned
     * with the status {@code SENDING}.
     */
    @Transactional
    public List<Outbox> claimDue(Instant now, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        List<Outbox> due = outboxes.findByStatusAndScheduledAtBeforeOrderByScheduledAtAsc(OutboxStatus.SCHEDULED, now);
        List<Outbox> claimed = new ArrayList<>();
        for (Outbox candidate : due) {
            if (claimed.size() >= limit) {
                break;
            }
            Outbox row = outboxes.findClaimable(candidate.getId(), OutboxStatus.SCHEDULED).orElse(null);
            if (row == null) {
                continue;
            }
            if (outboxes.claim(row.getId(), OutboxStatus.SCHEDULED, OutboxStatus.SENDING, now) != 1) {
                // Another worker won the row first. Skip it rather than sending twice.
                continue;
            }
            row.setStatus(OutboxStatus.SENDING);
            row.setAttemptCount(row.getAttemptCount() + 1);
            row.setUpdatedAt(now);
            claimed.add(row);
        }
        return claimed;
    }

    /** Marks a claimed row as delivered. */
    @Transactional
    public void markSent(Long id) {
        Outbox row = outboxes.findById(id).orElseThrow(() -> NotFoundException.of("Outbox row", id));
        row.setStatus(OutboxStatus.SENT);
        row.setErrorMessage(null);
        row.setUpdatedAt(Instant.now());
        outboxes.save(row);
    }

    /** Marks a claimed row as failed and records why. */
    @Transactional
    public void markFailed(Long id, String error) {
        Outbox row = outboxes.findById(id).orElseThrow(() -> NotFoundException.of("Outbox row", id));
        row.setStatus(OutboxStatus.FAILED);
        row.setErrorMessage(error);
        row.setUpdatedAt(Instant.now());
        outboxes.save(row);
    }

    private static OutboxDto toDto(Outbox row) {
        return new OutboxDto(
                row.getId(),
                row.getFromAccountId(),
                row.getToEmail(),
                row.getSubject(),
                row.getScheduledAt(),
                row.getStatus(),
                row.getAttemptCount(),
                row.getErrorMessage());
    }
}
