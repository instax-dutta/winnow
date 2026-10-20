package io.github.instaxdutta.winnow.repository;

import io.github.instaxdutta.winnow.domain.Delivery;
import io.github.instaxdutta.winnow.domain.DeliveryStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    List<Delivery> findByCampaignId(Long campaignId);

    List<Delivery> findByCampaignIdAndStatus(Long campaignId, DeliveryStatus status);

    long countByCampaignIdAndStatus(Long campaignId, DeliveryStatus status);
}
