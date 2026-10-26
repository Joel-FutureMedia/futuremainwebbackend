package com.futuremedia.repositories;

import com.futuremedia.models.CampaignStatus;
import com.futuremedia.models.EmailCampaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmailCampaignRepository extends JpaRepository<EmailCampaign, Long> {

    List<EmailCampaign> findAllByOrderByCreatedDateDesc();

    List<EmailCampaign> findByStatusOrderByCreatedDateDesc(CampaignStatus status);

    long countByStatus(CampaignStatus status);
}
