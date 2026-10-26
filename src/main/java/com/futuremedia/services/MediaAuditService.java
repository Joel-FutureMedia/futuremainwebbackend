package com.futuremedia.services;

import com.futuremedia.models.BadRequestException;
import com.futuremedia.models.MediaAudit;
import com.futuremedia.models.ResourceNotFoundException;
import com.futuremedia.repositories.MediaAuditRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class MediaAuditService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final Set<String> BUSINESS_OPTIONS = Set.of(
            "Brand awareness",
            "Product Launch",
            "Promotions & specials",
            "Event promotion",
            "Retail / In-store",
            "Digital",
            "Not sure yet"
    );

    private static final Set<String> CAMPAIGN_TYPE_OPTIONS = Set.of(
            "Retail",
            "FMCG",
            "Financial Services",
            "Government/Parastatal",
            "Media Agency",
            "Hospitality",
            "Healthcare",
            "Professional Services",
            "Other"
    );

    private static final Set<String> AUDIENCE_OPTIONS = Set.of(
            "Mass market consumers",
            "Working adults (Middle income)",
            "Affluent consumers (High income)",
            "Youth & Young Adults (18-25)",
            "Families & households",
            "Business owners & decision makers",
            "Not sure yet"
    );

    private static final Set<String> ADVERTISING_OPTIONS = Set.of(
            "Radio",
            "TV",
            "Social media",
            "Influencer marketing",
            "Online Display or Search Ads",
            "Billboards / DOOH",
            "Print",
            "Nothing Yet"
    );

    private static final Set<String> TIMELINE_OPTIONS = Set.of(
            "As soon as possible",
            "Within 1-3 months",
            "3-6 months' time",
            "Planning for next year",
            "Not sure yet"
    );

    private static final Set<String> BUDGET_OPTIONS = Set.of(
            "Under N$20,000",
            "N$20,000 – N$50,000",
            "N$50,000 – N$150,000",
            "N$150,000+",
            "Prefer not to say"
    );

    @Autowired
    private MediaAuditRepository mediaAuditRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private LeadService leadService;

    public MediaAudit submit(Map<String, Object> body) {
        if (body == null) {
            throw new BadRequestException("Request body is required");
        }

        String name = requiredText(body.get("name"), "Name is required");
        String email = requiredText(body.get("email"), "Valid email is required");
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new BadRequestException("Valid email is required");
        }
        String telephone = requiredText(body.get("telephone"), "Telephone is required");
        String company = requiredText(body.get("company"), "Company you represent is required");
        String campaignName = requiredText(body.get("campaignName"), "Campaign name is required");
        String additionalDetails = optionalText(body.get("additionalDetails"));

        List<String> businessDescription = requiredOptions(
                body.get("businessDescription"), BUSINESS_OPTIONS,
                "Please select what best describes your business");
        String campaignType = requiredOption(
                body.get("campaignType"), CAMPAIGN_TYPE_OPTIONS,
                "Please select the type of campaign you are planning");
        String campaignTypeOther = optionalText(body.get("campaignTypeOther"));
        if ("Other".equals(campaignType) && (campaignTypeOther == null || campaignTypeOther.isBlank())) {
            throw new BadRequestException("Please tell us what type of campaign you are planning");
        }
        if (!"Other".equals(campaignType)) {
            campaignTypeOther = null;
        }

        String locationCountry = requiredText(body.get("locationCountry"), "Location is required");
        String locationTown = firstText(body.get("locationTown"), body.get("otherTown"), body.get("otherCountry"));
        String location = optionalText(body.get("location"));
        location = resolveLocation(locationCountry, locationTown, location);

        String primaryAudience = requiredOption(
                body.get("primaryAudience"), AUDIENCE_OPTIONS,
                "Please select your primary audience");

        List<String> currentAdvertising = requiredOptions(
                body.get("currentAdvertising"), ADVERTISING_OPTIONS,
                "Please select the advertising you currently do");
        if (currentAdvertising.contains("Nothing Yet") && currentAdvertising.size() > 1) {
            currentAdvertising = List.of("Nothing Yet");
        }

        String advertiseTimeline = requiredOption(
                body.get("advertiseTimeline"), TIMELINE_OPTIONS,
                "Please select when you are looking to advertise");

        String advertisingBudget = optionalText(body.get("advertisingBudget"));
        if (advertisingBudget != null && !BUDGET_OPTIONS.contains(advertisingBudget)) {
            throw new BadRequestException("Please choose a valid advertising budget, or skip this step");
        }

        MediaAudit audit = new MediaAudit();
        audit.setName(name);
        audit.setEmail(email.toLowerCase());
        audit.setTelephone(telephone);
        audit.setCompany(company);
        audit.setCampaignName(campaignName);
        audit.setLocation(location);
        audit.setAdditionalDetails(additionalDetails);
        audit.setBusinessDescription(join(businessDescription));
        audit.setCampaignType(campaignType);
        audit.setCampaignTypeOther(campaignTypeOther);
        audit.setLocationCountry(normalizeCountry(locationCountry));
        audit.setLocationTown(locationTown);
        audit.setPrimaryAudience(primaryAudience);
        audit.setCurrentAdvertising(join(currentAdvertising));
        audit.setAdvertiseTimeline(advertiseTimeline);
        audit.setAdvertisingBudget(advertisingBudget);
        MediaAudit saved = mediaAuditRepository.save(audit);

        leadService.captureQuietly(saved.getName(), saved.getEmail(), saved.getTelephone(),
                saved.getCompany(), "Media Audit");
        emailService.sendMediaAuditEmail(saved);
        return saved;
    }

    public List<MediaAudit> getAll() {
        return mediaAuditRepository.findAllByOrderBySubmittedDateDesc();
    }

    public MediaAudit getById(Long id) {
        return mediaAuditRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Media audit not found with id: " + id));
    }

    public void delete(Long id) {
        MediaAudit audit = getById(id);
        mediaAuditRepository.delete(audit);
    }

    private String resolveLocation(String locationCountry, String locationTown, String location) {
        String country = normalizeCountry(locationCountry);
        if ("Namibia".equals(country)) {
            String town = requiredText(locationTown, "Please choose a Namibian town");
            if ("Other town".equalsIgnoreCase(town)) {
                throw new BadRequestException("Please enter your town");
            }
            return town + ", Namibia";
        }
        if ("South Africa".equals(country)) {
            return "South Africa";
        }
        if ("Other country".equals(country)) {
            String otherCountry = requiredText(locationTown, "Please enter your country");
            return otherCountry;
        }
        if (location != null && !location.isBlank()) {
            return location;
        }
        throw new BadRequestException("Please choose Namibia, South Africa, or another country");
    }

    private String normalizeCountry(String locationCountry) {
        if (locationCountry == null) {
            return null;
        }
        String value = locationCountry.trim();
        if (value.equalsIgnoreCase("Namibia")) {
            return "Namibia";
        }
        if (value.equalsIgnoreCase("South Africa")) {
            return "South Africa";
        }
        if (value.equalsIgnoreCase("Other country") || value.equalsIgnoreCase("Other")) {
            return "Other country";
        }
        return value;
    }

    private String requiredText(Object value, String message) {
        String text = optionalText(value);
        if (text == null) {
            throw new BadRequestException(message);
        }
        return text;
    }

    private String optionalText(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isBlank() ? null : text;
    }

    private String firstText(Object... values) {
        for (Object value : values) {
            String text = optionalText(value);
            if (text != null) {
                return text;
            }
        }
        return null;
    }

    private String requiredOption(Object value, Set<String> allowed, String message) {
        String text = requiredText(value, message);
        if (!allowed.contains(text)) {
            throw new BadRequestException(message);
        }
        return text;
    }

    private List<String> requiredOptions(Object value, Set<String> allowed, String message) {
        List<String> values = toList(value);
        if (values.isEmpty()) {
            throw new BadRequestException(message);
        }
        for (String item : values) {
            if (!allowed.contains(item)) {
                throw new BadRequestException(message);
            }
        }
        return values;
    }

    private List<String> toList(Object value) {
        LinkedHashSet<String> items = new LinkedHashSet<>();
        if (value == null) {
            return new ArrayList<>();
        }
        if (value instanceof Collection<?> collection) {
            for (Object item : collection) {
                addListItem(items, item);
            }
        } else {
            String text = value.toString().trim();
            if (!text.isBlank()) {
                if (text.startsWith("[") && text.endsWith("]")) {
                    text = text.substring(1, text.length() - 1);
                }
                for (String part : text.split("\\s*[;,]\\s*")) {
                    addListItem(items, part);
                }
            }
        }
        return new ArrayList<>(items);
    }

    private void addListItem(Set<String> items, Object item) {
        if (item == null) {
            return;
        }
        String text = item.toString().trim();
        if (!text.isBlank()) {
            items.add(text);
        }
    }

    private String join(List<String> values) {
        return values.stream().collect(Collectors.joining("; "));
    }
}
