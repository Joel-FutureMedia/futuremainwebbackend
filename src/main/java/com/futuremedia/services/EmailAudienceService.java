package com.futuremedia.services;

import com.futuremedia.models.CapturedEmail;
import com.futuremedia.models.ContactUs;
import com.futuremedia.models.EmailAudienceType;
import com.futuremedia.models.GatedDocumentRequest;
import com.futuremedia.models.Lead;
import com.futuremedia.models.LeadSource;
import com.futuremedia.models.MediaAudit;
import com.futuremedia.models.ResourceNotFoundException;
import com.futuremedia.models.ShopbeatDemoRequest;
import com.futuremedia.models.Subscription;
import com.futuremedia.repositories.ContactUsRepository;
import com.futuremedia.repositories.GatedDocumentRequestRepository;
import com.futuremedia.repositories.LeadRepository;
import com.futuremedia.repositories.LeadSourceRepository;
import com.futuremedia.repositories.MediaAuditRepository;
import com.futuremedia.repositories.ShopbeatDemoRequestRepository;
import com.futuremedia.repositories.SubscriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class EmailAudienceService {

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private LeadRepository leadRepository;

    @Autowired
    private LeadSourceRepository leadSourceRepository;

    @Autowired
    private ContactUsRepository contactUsRepository;

    @Autowired
    private MediaAuditRepository mediaAuditRepository;

    @Autowired
    private ShopbeatDemoRequestRepository shopbeatDemoRequestRepository;

    @Autowired
    private GatedDocumentRequestRepository gatedDocumentRequestRepository;

    public List<CapturedEmail> collect(EmailAudienceType audience, Long leadSourceId) {
        List<CapturedEmail> contacts = new ArrayList<>();
        EmailAudienceType type = audience != null ? audience : EmailAudienceType.ALL_CAPTURED;

        switch (type) {
            case SUBSCRIBERS -> addSubscribers(contacts);
            case LEADS -> addLeads(contacts, null);
            case CONTACT_US -> addContactUs(contacts);
            case MEDIA_AUDIT -> addMediaAudits(contacts);
            case SHOPBEAT -> addShopbeat(contacts);
            case GATED_DOWNLOADS -> addGatedDownloads(contacts);
            case LEAD_SOURCE -> addLeads(contacts, leadSourceId);
            case ALL_CAPTURED -> {
                addSubscribers(contacts);
                addLeads(contacts, null);
                addContactUs(contacts);
                addMediaAudits(contacts);
                addShopbeat(contacts);
                addGatedDownloads(contacts);
            }
        }

        return deduplicate(contacts);
    }

    public Map<String, Object> preview(EmailAudienceType audience, Long leadSourceId) {
        List<CapturedEmail> contacts = collect(audience, leadSourceId);
        List<CapturedEmail> sample = contacts.size() > 100 ? contacts.subList(0, 100) : contacts;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("audience", audience != null ? audience.name() : EmailAudienceType.ALL_CAPTURED.name());
        result.put("total", contacts.size());
        result.put("contacts", sample);
        return result;
    }

    private void addSubscribers(List<CapturedEmail> contacts) {
        for (Subscription subscription : subscriptionRepository.findByActiveTrue()) {
            contacts.add(new CapturedEmail(
                    subscription.getFullName(),
                    subscription.getEmail(),
                    null,
                    "Subscribers"
            ));
        }
    }

    private void addLeads(List<CapturedEmail> contacts, Long leadSourceId) {
        List<Lead> leads;
        if (leadSourceId != null) {
            LeadSource source = leadSourceRepository.findById(leadSourceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Lead source not found with id: " + leadSourceId));
            leads = leadRepository.findBySourceOrderByCapturedDateDesc(source);
        } else {
            leads = leadRepository.findAllByOrderByCapturedDateDesc();
        }
        for (Lead lead : leads) {
            String sourceName = lead.getSource() != null ? lead.getSource().getName() : "Leads";
            contacts.add(new CapturedEmail(lead.getName(), lead.getEmail(), lead.getCompany(), sourceName));
        }
    }

    private void addContactUs(List<CapturedEmail> contacts) {
        for (ContactUs contact : contactUsRepository.findAllByOrderBySubmittedDateDesc()) {
            contacts.add(new CapturedEmail(contact.getName(), contact.getEmail(), contact.getCompany(), "Contact Us"));
        }
    }

    private void addMediaAudits(List<CapturedEmail> contacts) {
        for (MediaAudit audit : mediaAuditRepository.findAllByOrderBySubmittedDateDesc()) {
            contacts.add(new CapturedEmail(audit.getName(), audit.getEmail(), audit.getCompany(), "Media Audit"));
        }
    }

    private void addShopbeat(List<CapturedEmail> contacts) {
        for (ShopbeatDemoRequest request : shopbeatDemoRequestRepository.findAllByOrderBySubmittedDateDesc()) {
            contacts.add(new CapturedEmail(request.getName(), request.getEmail(), request.getCompany(), "Shopbeat"));
        }
    }

    private void addGatedDownloads(List<CapturedEmail> contacts) {
        for (GatedDocumentRequest request : gatedDocumentRequestRepository.findAllByOrderBySubmittedDateDesc()) {
            contacts.add(new CapturedEmail(request.getName(), request.getEmail(), request.getCompany(), "Gate Document Download"));
        }
    }

    private List<CapturedEmail> deduplicate(List<CapturedEmail> contacts) {
        Map<String, CapturedEmail> unique = new LinkedHashMap<>();
        for (CapturedEmail contact : contacts) {
            if (contact.getEmail() == null || contact.getEmail().isBlank()) {
                continue;
            }
            String key = contact.getEmail().trim().toLowerCase(Locale.ROOT);
            unique.putIfAbsent(key, new CapturedEmail(
                    contact.getName(),
                    key,
                    contact.getCompany(),
                    contact.getSource()
            ));
        }
        return new ArrayList<>(unique.values());
    }
}
