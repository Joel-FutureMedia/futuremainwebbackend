package com.futuremedia.repositories;

import com.futuremedia.models.EmailCampaignImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmailCampaignImageRepository extends JpaRepository<EmailCampaignImage, Long> {

    List<EmailCampaignImage> findByCampaign_IdOrderBySortOrderAscIdAsc(Long campaignId);
}
