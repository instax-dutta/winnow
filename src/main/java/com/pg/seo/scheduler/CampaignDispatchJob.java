package com.pg.seo.scheduler;

import com.pg.seo.service.CampaignService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Sends the campaigns that are waiting to go out.
 *
 * <p>A campaign is created as {@code QUEUED} together with one delivery row per recipient.
 * This job claims the queued campaigns, sends every recipient and records the outcome.
 *
 * <p>The claim is a conditional update inside the service, so two instances of the
 * application running at the same time cannot send the same campaign twice. A campaign
 * that was claimed by another worker is simply skipped.
 *
 * <p>One failing campaign does not stop the others, and one failing recipient does not
 * fail the campaign: the failure is recorded on the delivery row and the campaign ends as
 * partial or failed.
 */
@Component
public class CampaignDispatchJob {

    private static final Logger LOG = LoggerFactory.getLogger(CampaignDispatchJob.class);

    /** How many campaigns one pass claims. */
    private final int batchSize;

    private final CampaignService campaigns;

    public CampaignDispatchJob(
            CampaignService campaigns,
            @Value("${seo.scheduler.campaign-batch-size:10}") int batchSize) {
        this.campaigns = campaigns;
        this.batchSize = Math.max(batchSize, 1);
    }

    /**
     * Claims the queued campaigns and sends them.
     */
    @Scheduled(fixedDelayString = "${seo.scheduler.campaign-delay-ms:30000}")
    public void dispatch() {
        List<Long> claimed = campaigns.claimQueued(batchSize);
        if (claimed.isEmpty()) {
            return;
        }
        LOG.info("Dispatching {} campaign(s)", claimed.size());
        for (Long campaignId : claimed) {
            try {
                campaigns.dispatch(campaignId);
            } catch (RuntimeException e) {
                LOG.error("The campaign {} could not be dispatched ({})",
                        campaignId, e.getClass().getSimpleName());
            }
        }
    }
}
