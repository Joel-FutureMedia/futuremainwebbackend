package com.futuremedia.repositories;

import com.futuremedia.models.CampaignRecipientStatus;
import com.futuremedia.models.EmailCampaign;
import com.futuremedia.models.EmailCampaignRecipient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmailCampaignRecipientRepository extends JpaRepository<EmailCampaignRecipient, Long> {

    List<EmailCampaignRecipient> findByCampaignOrderByIdAsc(EmailCampaign campaign);

    List<EmailCampaignRecipient> findByCampaignAndStatusInOrderByIdAsc(
            EmailCampaign campaign, List<CampaignRecipientStatus> statuses);

    long countByCampaign(EmailCampaign campaign);
}
