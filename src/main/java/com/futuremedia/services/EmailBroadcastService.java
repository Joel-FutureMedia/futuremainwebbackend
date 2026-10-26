package com.futuremedia.services;

import com.futuremedia.models.CampaignRecipientStatus;
import com.futuremedia.models.CampaignStatus;
import com.futuremedia.models.EmailCampaign;
import com.futuremedia.models.EmailCampaignImage;
import com.futuremedia.models.EmailCampaignRecipient;
import com.futuremedia.models.EmailTemplate;
import com.futuremedia.repositories.EmailCampaignImageRepository;
import com.futuremedia.repositories.EmailCampaignRecipientRepository;
import com.futuremedia.repositories.EmailCampaignRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EmailBroadcastService {

    @Autowired
    private EmailCampaignRepository emailCampaignRepository;

    @Autowired
    private EmailCampaignRecipientRepository emailCampaignRecipientRepository;

    @Autowired
    private EmailCampaignImageRepository emailCampaignImageRepository;

    @Autowired
    private EmailTemplateService emailTemplateService;

    @Autowired
    private EmailService emailService;

    @Async("emailTaskExecutor")
    @Transactional
    public void sendCampaignAsync(Long campaignId) {
        EmailCampaign campaign = emailCampaignRepository.findById(campaignId).orElse(null);
        if (campaign == null) {
            System.err.println("Email campaign not found for async send: " + campaignId);
            return;
        }

        List<EmailCampaignRecipient> recipients = emailCampaignRecipientRepository
                .findByCampaignAndStatusInOrderByIdAsc(
                        campaign,
                        List.of(CampaignRecipientStatus.PENDING, CampaignRecipientStatus.FAILED)
                );

        System.out.println("Starting async email campaign id=" + campaign.getId()
                + " '" + campaign.getName() + "' to " + recipients.size() + " recipients...");

        int sent = campaign.getSentCount();
        int failed = 0;

        EmailTemplate template = emailTemplateService.getById(campaign.getTemplate().getId());
        List<EmailCampaignImage> images = emailCampaignImageRepository
                .findByCampaign_IdOrderBySortOrderAscIdAsc(campaignId);

        for (EmailCampaignRecipient recipient : recipients) {
            try {
                String html = emailTemplateService.renderHtml(
                        template,
                        recipient.getName(),
                        recipient.getEmail(),
                        recipient.getCompany(),
                        images
                );
                emailService.sendMarketingEmail(recipient.getEmail(), campaign.getSubject(), html);
                recipient.setStatus(CampaignRecipientStatus.SENT);
                recipient.setErrorMessage(null);
                recipient.setSentAt(LocalDateTime.now());
                sent++;
            } catch (Exception e) {
                recipient.setStatus(CampaignRecipientStatus.FAILED);
                recipient.setErrorMessage(e.getMessage());
                failed++;
                System.err.println("Campaign " + campaign.getId() + " failed for "
                        + recipient.getEmail() + ": " + e.getMessage());
            }
            emailCampaignRecipientRepository.save(recipient);

            campaign.setSentCount(sent);
            campaign.setFailedCount(failed);
            emailCampaignRepository.save(campaign);
        }

        campaign.setSentCount(sent);
        campaign.setFailedCount(failed);
        campaign.setCompletedAt(LocalDateTime.now());
        if (sent == 0 && failed > 0) {
            campaign.setStatus(CampaignStatus.FAILED);
        } else {
            campaign.setStatus(CampaignStatus.SENT);
        }
        emailCampaignRepository.save(campaign);

        System.out.println("Email campaign id=" + campaign.getId()
                + " completed. Sent=" + sent + ", Failed=" + failed);
    }
}
