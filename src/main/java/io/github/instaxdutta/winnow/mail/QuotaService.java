package io.github.instaxdutta.winnow.mail;

import io.github.instaxdutta.winnow.config.QuotaProperties;
import io.github.instaxdutta.winnow.domain.Campaign;
import io.github.instaxdutta.winnow.domain.Delivery;
import io.github.instaxdutta.winnow.domain.DeliveryStatus;
import io.github.instaxdutta.winnow.domain.QuotaExceededException;
import io.github.instaxdutta.winnow.repository.CampaignRepository;
import io.github.instaxdutta.winnow.repository.DeliveryRepository;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;

/**
 * Counts what a sending account has sent and refuses a send that would break a limit.
 *
 * <p>The limits are per account and per tenant, and they are separate for the hour, the
 * day and the month. A single cap of 50,000 a month labelled as a daily cap, with no
 * pacing per account at all, lets one account send far more than it is allowed to.
 *
 * <p>The windows are calendar windows in UTC.
 */
@Service
public class QuotaService {

    private final DeliveryRepository deliveries;
    private final CampaignRepository campaigns;
    private final QuotaProperties quota;

    public QuotaService(DeliveryRepository deliveries, CampaignRepository campaigns, QuotaProperties quota) {
        this.deliveries = deliveries;
        this.campaigns = campaigns;
        this.quota = quota;
    }

    /**
     * Counts the deliveries of one status in a window.
     *
     * @param accountId the sending account, or null for every account of the tenant
     */
    public long countByStatus(
            Long ownerUserId, Long accountId, DeliveryStatus status, Instant from, Instant to) {
        long count = 0;
        for (Campaign campaign : campaigns.findByOwnerUserIdOrderByCreatedAtDesc(ownerUserId)) {
            if (accountId != null && !accountId.equals(campaign.getFromAccountId())) {
                continue;
            }
            if (campaign.getCreatedAt() != null && campaign.getCreatedAt().isAfter(to)) {
                continue;
            }
            if (campaign.getFinishedAt() != null && campaign.getFinishedAt().isBefore(from)) {
                continue;
            }
            for (Delivery delivery : deliveries.findByCampaignIdAndStatus(campaign.getId(), status)) {
                // A failed delivery has no sent time, so the time it was recorded is used.
                Instant at = delivery.getSentAt() != null ? delivery.getSentAt() : delivery.getCreatedAt();
                if (at != null && !at.isBefore(from) && !at.isAfter(to)) {
                    count++;
                }
            }
        }
        return count;
    }

    /** The messages sent by the account since the start of the current hour. */
    public long sentThisHour(Long ownerUserId, Long accountId) {
        Instant now = Instant.now();
        return countByStatus(ownerUserId, accountId, DeliveryStatus.SENT, now.truncatedTo(ChronoUnit.HOURS), now);
    }

    /** The messages sent by the account since the start of the current day. */
    public long sentToday(Long ownerUserId, Long accountId) {
        Instant now = Instant.now();
        return countByStatus(ownerUserId, accountId, DeliveryStatus.SENT, now.truncatedTo(ChronoUnit.DAYS), now);
    }

    /** The messages sent by the account since the start of the current month. */
    public long sentThisMonth(Long ownerUserId, Long accountId) {
        Instant now = Instant.now();
        Instant startOfMonth = now.truncatedTo(ChronoUnit.DAYS).atZone(ZoneOffset.UTC).withDayOfMonth(1).toInstant();
        return countByStatus(ownerUserId, accountId, DeliveryStatus.SENT, startOfMonth, now);
    }

    /**
     * Refuses the send when the account has used up one of its limits.
     *
     * @throws QuotaExceededException when a limit is reached
     */
    public void assertWithinQuota(Long ownerUserId, Long accountId) {
        check(quota.hourlySendLimit(), sentThisHour(ownerUserId, accountId), "hourly");
        check(quota.dailySendLimit(), sentToday(ownerUserId, accountId), "daily");
        check(quota.monthlySendLimit(), sentThisMonth(ownerUserId, accountId), "monthly");
    }

    private void check(int limit, long used, String window) {
        if (limit <= 0) {
            return;
        }
        if (used >= limit) {
            throw new QuotaExceededException(
                    "The " + window + " send limit of " + limit + " messages has been reached");
        }
    }
}
