package com.pg.seo.repository;

import com.pg.seo.domain.Delivery;
import com.pg.seo.domain.DeliveryStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    List<Delivery> findByCampaignId(Long campaignId);

    List<Delivery> findByCampaignIdAndStatus(Long campaignId, DeliveryStatus status);

    long countByCampaignIdAndStatus(Long campaignId, DeliveryStatus status);
}
