package com.futuremedia.services;

import com.futuremedia.models.BadRequestException;
import com.futuremedia.models.CampaignRecipientStatus;
import com.futuremedia.models.CampaignStatus;
import com.futuremedia.models.CapturedEmail;
import com.futuremedia.models.EmailAudienceType;
import com.futuremedia.models.EmailCampaign;
import com.futuremedia.models.EmailCampaignImage;
import com.futuremedia.models.EmailCampaignRecipient;
import com.futuremedia.models.EmailTemplate;
import com.futuremedia.models.LeadSource;
import com.futuremedia.models.ResourceNotFoundException;
import com.futuremedia.models.User;
import com.futuremedia.repositories.EmailCampaignImageRepository;
import com.futuremedia.repositories.EmailCampaignRecipientRepository;
import com.futuremedia.repositories.EmailCampaignRepository;
import com.futuremedia.repositories.LeadSourceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class EmailCampaignService {

    @Autowired
    private EmailCampaignRepository emailCampaignRepository;

    @Autowired
    private EmailCampaignRecipientRepository emailCampaignRecipientRepository;

    @Autowired
    private EmailTemplateService emailTemplateService;

    @Autowired
    private EmailAudienceService emailAudienceService;

    @Autowired
    private EmailBroadcastService emailBroadcastService;

    @Autowired
    private LeadSourceRepository leadSourceRepository;

    @Autowired
    private EmailCampaignImageRepository emailCampaignImageRepository;

    @Autowired
    private FileStorageService fileStorageService;

    public EmailCampaign create(Map<String, Object> body, User currentUser) {
        String name = required(body.get("name"), "Campaign name is required");
        String subject = required(body.get("subject"), "Email subject is required");
        Long templateId = asLong(body.get("templateId"), "Template is required");
        EmailTemplate template = emailTemplateService.getById(templateId);
        if (!template.isActive()) {
            throw new BadRequestException("Selected template is not active");
        }

        EmailAudienceType audience = parseAudience(body.get("audience"));
        LeadSource leadSource = null;
        if (audience == EmailAudienceType.LEAD_SOURCE) {
            Long sourceId = asLong(body.get("leadSourceId"), "Lead source is required for LEAD_SOURCE audience");
            leadSource = leadSourceRepository.findById(sourceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Lead source not found with id: " + sourceId));
        }

        EmailCampaign campaign = new EmailCampaign();
        campaign.setName(name);
        campaign.setSubject(subject);
        campaign.setTemplate(template);
        campaign.setAudience(audience);
        campaign.setLeadSource(leadSource);
        campaign.setStatus(CampaignStatus.DRAFT);
        campaign.setCreatedBy(currentUser);
        return emailCampaignRepository.save(campaign);
    }

    public EmailCampaign updateDraft(Long id, Map<String, Object> body) {
        EmailCampaign campaign = getById(id);
        if (campaign.getStatus() != CampaignStatus.DRAFT) {
            throw new BadRequestException("Only draft campaigns can be edited");
        }
        if (body.get("name") != null && !body.get("name").toString().isBlank()) {
            campaign.setName(body.get("name").toString().trim());
        }
        if (body.get("subject") != null && !body.get("subject").toString().isBlank()) {
            campaign.setSubject(body.get("subject").toString().trim());
        }
        if (body.get("templateId") != null) {
            EmailTemplate template = emailTemplateService.getById(asLong(body.get("templateId"), "Template is required"));
            campaign.setTemplate(template);
        }
        if (body.get("audience") != null) {
            campaign.setAudience(parseAudience(body.get("audience")));
        }
        if (campaign.getAudience() == EmailAudienceType.LEAD_SOURCE && body.get("leadSourceId") != null) {
            Long sourceId = asLong(body.get("leadSourceId"), "Lead source is required");
            campaign.setLeadSource(leadSourceRepository.findById(sourceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Lead source not found with id: " + sourceId)));
        }
        return emailCampaignRepository.save(campaign);
    }

    public EmailCampaign broadcast(Long id) {
        EmailCampaign campaign = getById(id);
        if (campaign.getStatus() == CampaignStatus.SENDING) {
            throw new BadRequestException("This campaign is already sending");
        }
        if (campaign.getStatus() == CampaignStatus.SENT) {
            throw new BadRequestException("This campaign has already been sent. Create a new campaign to broadcast again.");
        }

        snapshotRecipients(campaign);
        campaign = getById(id);

        long pending = emailCampaignRecipientRepository
                .findByCampaignAndStatusInOrderByIdAsc(campaign, List.of(CampaignRecipientStatus.PENDING, CampaignRecipientStatus.FAILED))
                .size();
        if (pending == 0) {
            throw new BadRequestException("No captured emails found for the selected audience");
        }

        campaign.setStatus(CampaignStatus.SENDING);
        campaign.setStartedAt(LocalDateTime.now());
        campaign.setCompletedAt(null);
        EmailCampaign saved = emailCampaignRepository.save(campaign);
        emailBroadcastService.sendCampaignAsync(saved.getId());
        return saved;
    }

    public Map<String, Object> previewAudience(String audience, Long leadSourceId) {
        return emailAudienceService.preview(parseAudience(audience), leadSourceId);
    }

    public List<EmailCampaign> getAll() {
        return emailCampaignRepository.findAllByOrderByCreatedDateDesc();
    }

    @Transactional(readOnly = true)
    public EmailCampaign getById(Long id) {
        EmailCampaign campaign = emailCampaignRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with id: " + id));
        if (campaign.getImages() != null) {
            campaign.getImages().size();
        }
        return campaign;
    }

    public List<EmailCampaignRecipient> getRecipients(Long id) {
        return emailCampaignRecipientRepository.findByCampaignOrderByIdAsc(getById(id));
    }

    @Transactional
    public EmailCampaignImage addImage(Long campaignId, String caption, MultipartFile file) {
        EmailCampaign campaign = getById(campaignId);
        if (campaign.getStatus() != CampaignStatus.DRAFT) {
            throw new BadRequestException("Images can only be added to a draft campaign");
        }
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Image file is required");
        }
        EmailCampaignImage image = new EmailCampaignImage();
        image.setCampaign(campaign);
        image.setCaption(caption != null && !caption.isBlank() ? caption.trim() : null);
        image.setImageUrl(fileStorageService.storeImageOnly(file, "email-campaigns"));
        image.setSortOrder(campaign.getImages() == null ? 0 : campaign.getImages().size());
        EmailCampaignImage saved = emailCampaignImageRepository.save(image);
        if (campaign.getImages() != null) {
            campaign.getImages().add(saved);
        }
        return saved;
    }

    @Transactional
    public void deleteImage(Long campaignId, Long imageId) {
        EmailCampaign campaign = getById(campaignId);
        if (campaign.getStatus() != CampaignStatus.DRAFT) {
            throw new BadRequestException("Images can only be removed from a draft campaign");
        }
        EmailCampaignImage image = emailCampaignImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign image not found with id: " + imageId));
        if (image.getCampaign() == null || !image.getCampaign().getId().equals(campaign.getId())) {
            throw new BadRequestException("Image does not belong to this campaign");
        }
        fileStorageService.deleteFile(image.getImageUrl());
        if (campaign.getImages() != null) {
            campaign.getImages().remove(image);
        }
        emailCampaignImageRepository.delete(image);
    }

    @Transactional(readOnly = true)
    public String preview(Long id, String name, String email, String company) {
        EmailCampaign campaign = emailCampaignRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with id: " + id));
        if (campaign.getTemplate() == null || campaign.getTemplate().getId() == null) {
            throw new BadRequestException("This campaign has no email template");
        }
        EmailTemplate template = emailTemplateService.getById(campaign.getTemplate().getId());
        List<EmailCampaignImage> images = emailCampaignImageRepository
                .findByCampaign_IdOrderBySortOrderAscIdAsc(id);
        return emailTemplateService.renderHtml(
                template,
                name != null && !name.isBlank() ? name : "Joel",
                email != null && !email.isBlank() ? email : "joel@futuremedia.com",
                company != null && !company.isBlank() ? company : "Future Media",
                images
        );
    }

    @Transactional
    public void delete(Long id) {
        EmailCampaign campaign = getById(id);
        if (campaign.getStatus() == CampaignStatus.SENDING) {
            throw new BadRequestException("Cannot delete a campaign that is currently sending");
        }
        if (campaign.getImages() != null) {
            for (EmailCampaignImage image : campaign.getImages()) {
                fileStorageService.deleteFile(image.getImageUrl());
            }
        }
        emailCampaignRepository.delete(campaign);
    }

    private void snapshotRecipients(EmailCampaign campaign) {
        if (emailCampaignRecipientRepository.countByCampaign(campaign) > 0) {
            return;
        }
        Long leadSourceId = campaign.getLeadSource() != null ? campaign.getLeadSource().getId() : null;
        List<CapturedEmail> contacts = emailAudienceService.collect(campaign.getAudience(), leadSourceId);
        for (CapturedEmail contact : contacts) {
            EmailCampaignRecipient recipient = new EmailCampaignRecipient();
            recipient.setCampaign(campaign);
            recipient.setName(contact.getName() != null ? contact.getName() : "there");
            recipient.setEmail(contact.getEmail());
            recipient.setCompany(contact.getCompany());
            recipient.setSource(contact.getSource());
            recipient.setStatus(CampaignRecipientStatus.PENDING);
            emailCampaignRecipientRepository.save(recipient);
        }
        campaign.setTotalRecipients(contacts.size());
        campaign.setSentCount(0);
        campaign.setFailedCount(0);
        emailCampaignRepository.save(campaign);
    }

    private EmailAudienceType parseAudience(Object value) {
        if (value == null || value.toString().isBlank()) {
            return EmailAudienceType.ALL_CAPTURED;
        }
        try {
            return EmailAudienceType.valueOf(value.toString().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(
                    "Audience must be ALL_CAPTURED, SUBSCRIBERS, LEADS, CONTACT_US, MEDIA_AUDIT, SHOPBEAT, GATED_DOWNLOADS or LEAD_SOURCE");
        }
    }

    private String required(Object value, String message) {
        if (value == null || value.toString().isBlank()) {
            throw new BadRequestException(message);
        }
        return value.toString().trim();
    }

    private Long asLong(Object value, String message) {
        if (value == null || value.toString().isBlank()) {
            throw new BadRequestException(message);
        }
        try {
            return Long.valueOf(value.toString());
        } catch (NumberFormatException e) {
            throw new BadRequestException(message);
        }
    }
}
