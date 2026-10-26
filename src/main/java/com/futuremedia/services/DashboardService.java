package com.futuremedia.services;

import com.futuremedia.models.ApprovalStatus;
import com.futuremedia.models.CampaignStatus;
import com.futuremedia.models.ContactUs;
import com.futuremedia.models.JobApplication;
import com.futuremedia.models.Lead;
import com.futuremedia.models.MediaAudit;
import com.futuremedia.models.ShopbeatDemoRequest;
import com.futuremedia.models.Subscription;
import com.futuremedia.repositories.BannerRepository;
import com.futuremedia.repositories.ContactUsRepository;
import com.futuremedia.repositories.EmailCampaignRepository;
import com.futuremedia.repositories.EmailTemplateRepository;
import com.futuremedia.repositories.EventRepository;
import com.futuremedia.repositories.FreelancerApplicationRepository;
import com.futuremedia.repositories.FreelancerRoleRepository;
import com.futuremedia.repositories.GalleryRepository;
import com.futuremedia.repositories.GatedDocumentRepository;
import com.futuremedia.repositories.GatedDocumentRequestRepository;
import com.futuremedia.repositories.JobApplicationRepository;
import com.futuremedia.repositories.LeadRepository;
import com.futuremedia.repositories.MediaAuditRepository;
import com.futuremedia.repositories.NewsRepository;
import com.futuremedia.repositories.ShopbeatDemoRequestRepository;
import com.futuremedia.repositories.SubscriptionRepository;
import com.futuremedia.repositories.VacancyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    @Autowired private LeadRepository leadRepository;
    @Autowired private ContactUsRepository contactUsRepository;
    @Autowired private MediaAuditRepository mediaAuditRepository;
    @Autowired private ShopbeatDemoRequestRepository shopbeatDemoRequestRepository;
    @Autowired private SubscriptionRepository subscriptionRepository;
    @Autowired private GatedDocumentRequestRepository gatedDocumentRequestRepository;
    @Autowired private GatedDocumentRepository gatedDocumentRepository;
    @Autowired private JobApplicationRepository jobApplicationRepository;
    @Autowired private FreelancerApplicationRepository freelancerApplicationRepository;
    @Autowired private FreelancerRoleRepository freelancerRoleRepository;
    @Autowired private NewsRepository newsRepository;
    @Autowired private EventRepository eventRepository;
    @Autowired private GalleryRepository galleryRepository;
    @Autowired private VacancyRepository vacancyRepository;
    @Autowired private BannerRepository bannerRepository;
    @Autowired private EmailCampaignRepository emailCampaignRepository;
    @Autowired private EmailTemplateRepository emailTemplateRepository;

    public Map<String, Object> overview() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("kpis", kpis());
        payload.put("sources", sources());
        payload.put("pipeline", pipeline());
        payload.put("content", content());
        payload.put("recent", recent());
        return payload;
    }

    private Map<String, Object> kpis() {
        Map<String, Object> kpis = new LinkedHashMap<>();
        kpis.put("leads", leadRepository.count());
        kpis.put("contacts", contactUsRepository.count());
        kpis.put("mediaAudits", mediaAuditRepository.count());
        kpis.put("shopbeat", shopbeatDemoRequestRepository.count());
        kpis.put("subscribers", subscriptionRepository.count());
        kpis.put("activeSubscribers", subscriptionRepository.countByActiveTrue());
        kpis.put("applications", jobApplicationRepository.count());
        kpis.put("freelancerApplications", freelancerApplicationRepository.count());
        kpis.put("gatedRequests", gatedDocumentRequestRepository.count());
        kpis.put("capturedTotal",
                leadRepository.count()
                        + contactUsRepository.count()
                        + mediaAuditRepository.count()
                        + shopbeatDemoRequestRepository.count()
                        + gatedDocumentRequestRepository.count());
        return kpis;
    }

    private List<Map<String, Object>> sources() {
        List<Map<String, Object>> rows = new ArrayList<>();
        addSource(rows, "Leads", leadRepository.count());
        addSource(rows, "Contact Us", contactUsRepository.count());
        addSource(rows, "Media Audit", mediaAuditRepository.count());
        addSource(rows, "Shopbeat", shopbeatDemoRequestRepository.count());
        addSource(rows, "Newsletter", subscriptionRepository.count());
        addSource(rows, "Gated downloads", gatedDocumentRequestRepository.count());
        addSource(rows, "Job applications", jobApplicationRepository.count());
        addSource(rows, "Freelancer", freelancerApplicationRepository.count());
        for (Object[] row : leadRepository.countGroupedBySource()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", String.valueOf(row[0]));
            item.put("count", row[1]);
            item.put("kind", "lead-source");
            rows.add(item);
        }
        rows.sort(Comparator.comparingLong(item -> -((Number) item.get("count")).longValue()));
        return rows;
    }

    private void addSource(List<Map<String, Object>> rows, String name, long count) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("name", name);
        item.put("count", count);
        item.put("kind", "form");
        rows.add(item);
    }

    private List<Map<String, Object>> pipeline() {
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(pipe("Contact Us", contactUsRepository.count(), "contacts"));
        rows.add(pipe("Media Audit", mediaAuditRepository.count(), "audits"));
        rows.add(pipe("Shopbeat", shopbeatDemoRequestRepository.count(), "shopbeat"));
        rows.add(pipe("Leads", leadRepository.count(), "leads"));
        rows.add(pipe("Newsletter", subscriptionRepository.count(), "subscribers"));
        rows.add(pipe("Gated downloads", gatedDocumentRequestRepository.count(), "downloads"));
        rows.add(pipe("Applications", jobApplicationRepository.count(), "applications"));
        rows.add(pipe("Freelancers", freelancerApplicationRepository.count(), "freelancer-applications"));
        return rows;
    }

    private Map<String, Object> pipe(String name, long count, String id) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("name", name);
        item.put("count", count);
        item.put("id", id);
        return item;
    }

    private Map<String, Object> content() {
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("news", newsRepository.count());
        content.put("newsPending", newsRepository.countByApprovalStatus(ApprovalStatus.PENDING));
        content.put("newsApproved", newsRepository.countByApprovalStatus(ApprovalStatus.APPROVED));
        content.put("events", eventRepository.count());
        content.put("gallery", galleryRepository.count());
        content.put("vacancies", vacancyRepository.count());
        content.put("vacanciesLive", vacancyRepository.countByPublishedTrue());
        content.put("freelancerRoles", freelancerRoleRepository.count());
        content.put("freelancerRolesLive", freelancerRoleRepository.countByPublishedTrue());
        content.put("banners", bannerRepository.count());
        content.put("documents", gatedDocumentRepository.count());
        content.put("templates", emailTemplateRepository.count());
        content.put("campaigns", emailCampaignRepository.count());
        content.put("campaignsDraft", emailCampaignRepository.countByStatus(CampaignStatus.DRAFT));
        content.put("campaignsSent", emailCampaignRepository.countByStatus(CampaignStatus.SENT));
        return content;
    }

    private List<Map<String, Object>> recent() {
        List<Map<String, Object>> items = new ArrayList<>();
        take(leadRepository.findAllByOrderByCapturedDateDesc(), 8).forEach(lead ->
                items.add(activity("Lead", lead.getName(), lead.getEmail(), sourceName(lead), lead.getCapturedDate())));
        take(contactUsRepository.findAllByOrderBySubmittedDateDesc(), 6).forEach(contact ->
                items.add(activity("Contact Us", contact.getName(), contact.getEmail(), brandName(contact), contact.getSubmittedDate())));
        take(mediaAuditRepository.findAllByOrderBySubmittedDateDesc(), 6).forEach(audit ->
                items.add(activity("Media Audit", audit.getName(), audit.getEmail(), audit.getCompany(), audit.getSubmittedDate())));
        take(shopbeatDemoRequestRepository.findAllByOrderBySubmittedDateDesc(), 6).forEach(request ->
                items.add(activity("Shopbeat", request.getName(), request.getEmail(), request.getCompany(), request.getSubmittedDate())));
        take(subscriptionRepository.findAllByOrderBySubscribedDateDesc(), 6).forEach(sub ->
                items.add(activity("Newsletter", sub.getFullName(), sub.getEmail(), sub.getLanguage() != null ? sub.getLanguage().name() : "ENGLISH", sub.getSubscribedDate())));
        take(jobApplicationRepository.findAllByOrderByAppliedDateDesc(), 6).forEach(app ->
                items.add(activity("Application", app.getName() + " " + app.getSurname(), null, vacancyTitle(app), app.getAppliedDate())));
        take(freelancerApplicationRepository.findAllByOrderByAppliedDateDesc(), 6).forEach(app ->
                items.add(activity("Freelancer", app.getName() + " " + app.getSurname(), app.getEmail(),
                        app.getRole() != null ? app.getRole().getTitle() : "Freelancer", app.getAppliedDate())));
        items.sort(Comparator.comparing((Map<String, Object> item) -> (Comparable) item.get("sort")).reversed());
        return items.size() > 18 ? items.subList(0, 18) : items;
    }

    private Map<String, Object> activity(String source, String name, String email, String detail, LocalDateTime at) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("source", source);
        item.put("name", name);
        item.put("email", email);
        item.put("detail", detail);
        item.put("at", at);
        item.put("sort", at != null ? at : LocalDate.EPOCH.atStartOfDay());
        return item;
    }

    private <T> List<T> take(List<T> values, int limit) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.size() > limit ? values.subList(0, limit) : values;
    }

    private String sourceName(Lead lead) {
        return lead.getSource() != null ? lead.getSource().getName() : "Lead";
    }

    private String brandName(ContactUs contact) {
        return contact.getBrand() != null ? contact.getBrand().getName() : "Contact Us";
    }

    private String vacancyTitle(JobApplication application) {
        return application.getVacancy() != null ? application.getVacancy().getTitle() : "Vacancy";
    }
}
