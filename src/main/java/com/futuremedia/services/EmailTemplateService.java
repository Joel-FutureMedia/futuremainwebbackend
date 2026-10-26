package com.futuremedia.services;

import com.futuremedia.models.BadRequestException;
import com.futuremedia.models.EmailCampaignImage;
import com.futuremedia.models.EmailTemplate;
import com.futuremedia.models.EmailTemplateImage;
import com.futuremedia.models.ResourceNotFoundException;
import com.futuremedia.models.TemplateImageSection;
import com.futuremedia.models.User;
import com.futuremedia.repositories.EmailTemplateImageRepository;
import com.futuremedia.repositories.EmailTemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class EmailTemplateService {

    private static final Pattern HEX_COLOR = Pattern.compile("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$");

    @Autowired
    private EmailTemplateRepository emailTemplateRepository;

    @Autowired
    private EmailTemplateImageRepository emailTemplateImageRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private EmailService emailService;

    @Value("${app.logo.url}")
    private String logoUrl;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    public Map<String, String> defaults() {
        Map<String, String> defaults = new LinkedHashMap<>();
        defaults.put("headerColor", "#0b1f3a");
        defaults.put("headerTextColor", "#ffffff");
        defaults.put("bodyColor", "#ffffff");
        defaults.put("bodyTextColor", "#334155");
        defaults.put("footerColor", "#0b1f3a");
        defaults.put("footerTextColor", "#a8c0d8");
        defaults.put("ctaButtonColor", "#1a73e8");
        defaults.put("ctaTextColor", "#ffffff");
        defaults.put("logoUrl", logoUrl);
        defaults.put("placeholders", "{{name}}, {{email}}, {{company}}");
        return defaults;
    }

    public EmailTemplate create(Map<String, Object> body, User currentUser) {
        String name = required(body.get("name"), "Template name is required");
        if (emailTemplateRepository.existsByNameIgnoreCase(name)) {
            throw new BadRequestException("A template with this name already exists");
        }

        EmailTemplate template = new EmailTemplate();
        template.setName(name);
        applyFields(template, body);
        template.setCreatedBy(currentUser);
        return emailTemplateRepository.save(template);
    }

    public EmailTemplate update(Long id, Map<String, Object> body) {
        EmailTemplate template = getById(id);
        if (body.get("name") != null && !body.get("name").toString().isBlank()) {
            String name = body.get("name").toString().trim();
            emailTemplateRepository.findByNameIgnoreCase(name).ifPresent(existing -> {
                if (!existing.getId().equals(id)) {
                    throw new BadRequestException("A template with this name already exists");
                }
            });
            template.setName(name);
        }
        applyFields(template, body);
        return emailTemplateRepository.save(template);
    }

    @Transactional
    public EmailTemplateImage addImage(Long templateId, String section, String caption, MultipartFile file) {
        EmailTemplate template = getById(templateId);
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Image file is required");
        }
        TemplateImageSection imageSection = parseSection(section);

        EmailTemplateImage image = new EmailTemplateImage();
        image.setTemplate(template);
        image.setSection(imageSection);
        image.setCaption(caption != null && !caption.isBlank() ? caption.trim() : null);
        image.setImageUrl(fileStorageService.storeImageOnly(file, "email-templates"));
        image.setSortOrder(template.getImages() == null ? 0 : template.getImages().size());
        EmailTemplateImage saved = emailTemplateImageRepository.save(image);
        if (template.getImages() != null) {
            template.getImages().add(saved);
        }
        return saved;
    }

    @Transactional
    public void deleteImage(Long templateId, Long imageId) {
        EmailTemplate template = getById(templateId);
        EmailTemplateImage image = emailTemplateImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Template image not found with id: " + imageId));
        if (image.getTemplate() == null || !image.getTemplate().getId().equals(template.getId())) {
            throw new BadRequestException("Image does not belong to this template");
        }
        fileStorageService.deleteFile(image.getImageUrl());
        template.getImages().remove(image);
        emailTemplateImageRepository.delete(image);
    }

    @Transactional
    public void delete(Long id) {
        EmailTemplate template = getById(id);
        if (template.getImages() != null) {
            for (EmailTemplateImage image : template.getImages()) {
                fileStorageService.deleteFile(image.getImageUrl());
            }
        }
        emailTemplateRepository.delete(template);
    }

    public EmailTemplate getById(Long id) {
        return emailTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Email template not found with id: " + id));
    }

    public List<EmailTemplate> getAll() {
        return emailTemplateRepository.findAllByOrderByCreatedDateDesc();
    }

    public List<EmailTemplate> getActive() {
        return emailTemplateRepository.findByActiveTrueOrderByCreatedDateDesc();
    }

    public String preview(Long id, String name, String email, String company) {
        return renderHtml(getById(id),
                name != null && !name.isBlank() ? name : "Joel",
                email != null && !email.isBlank() ? email : "joel@futuremedia.com",
                company != null && !company.isBlank() ? company : "Future Media");
    }

    public void sendTest(Long id, String toEmail, String name, String company) {
        if (toEmail == null || toEmail.isBlank()) {
            throw new BadRequestException("Test recipient email is required");
        }
        EmailTemplate template = getById(id);
        String html = renderHtml(
                template,
                name != null && !name.isBlank() ? name : "Joel",
                toEmail.trim(),
                company != null && !company.isBlank() ? company : "Future Media"
        );
        emailService.sendMarketingEmail(toEmail.trim(), "Test: " + template.getName(), html);
    }

    public String renderHtml(EmailTemplate template, String name, String email, String company) {
        return renderHtml(template, name, email, company, List.of());
    }

    public String renderHtml(EmailTemplate template, String name, String email, String company,
                             List<EmailCampaignImage> campaignImages) {
        String headerColor = safeColor(template.getHeaderColor(), "#0b1f3a");
        String headerText = safeColor(template.getHeaderTextColor(), "#ffffff");
        String bodyColor = safeColor(template.getBodyColor(), "#ffffff");
        String bodyText = safeColor(template.getBodyTextColor(), "#334155");
        String footerColor = safeColor(template.getFooterColor(), "#0b1f3a");
        String footerText = safeColor(template.getFooterTextColor(), "#a8c0d8");
        String ctaBg = safeColor(template.getCtaButtonColor(), "#f08b8b");
        String ctaFg = safeColor(template.getCtaTextColor(), "#ffffff");

        String displayName = name != null ? name.trim() : "there";
        String firstName = firstName(displayName);

        String headerHeadline = personalize(template.getHeaderHeadline(), displayName, email, company);
        String headerSub = personalize(template.getHeaderSubheadline(), displayName, email, company);
        String bodyHeadline = personalize(template.getBodyHeadline(), displayName, email, company);
        String bodyContent = personalize(toHtmlParagraphs(template.getBodyContent()), displayName, email, company);
        String footer = personalize(toHtmlParagraphs(template.getFooterText()), displayName, email, company);
        String preheader = personalize(template.getPreheader(), displayName, email, company);

        String logoBlock = "";
        if (template.isUseCompanyLogo() && logoUrl != null && !logoUrl.isBlank()) {
            logoBlock = "<img src=\"" + logoUrl + "\" alt=\"Future Media\" width=\"140\" style=\"max-width:140px;height:auto;margin:0 auto 18px;display:block;border:0;\"/>";
        }

        String headerImages = headerHeroHtml(template, headerColor, headerText);
        String campaignGallery = campaignGalleryHtml(campaignImages, bodyText);
        String bodyImages = bodyGalleryHtml(template, bodyText);
        String footerImages = footerMarksHtml(template);

        String headerHeadlineHtml = isBlank(headerHeadline) ? ""
                : "<h1 style=\"color:" + headerText + ";margin:0;font-size:28px;font-weight:800;line-height:1.25;letter-spacing:-0.02em;\">" + headerHeadline + "</h1>";
        String headerSubHtml = isBlank(headerSub) ? ""
                : "<p style=\"color:" + headerText + ";opacity:0.88;margin:12px 0 0;font-size:15px;line-height:1.65;\">" + headerSub + "</p>";
        String bodyHeadlineHtml = isBlank(bodyHeadline) ? ""
                : "<h2 style=\"color:" + bodyText + ";margin:0 0 14px;font-size:22px;font-weight:800;line-height:1.3;\">" + bodyHeadline + "</h2>";
        String greeting = "<p style=\"color:" + bodyText + ";font-size:16px;margin:0 0 18px;\">Hi " + escapeHtml(firstName) + ",</p>";

        String ctaHtml = "";
        if (!isBlank(template.getCtaText()) && !isBlank(template.getCtaUrl())) {
            ctaHtml = "<table role=\"presentation\" cellspacing=\"0\" cellpadding=\"0\" align=\"center\" style=\"margin:28px auto 8px;\">"
                    + "<tr><td align=\"center\" bgcolor=\"" + ctaBg + "\" style=\"background:" + ctaBg
                    + ";border-radius:999px;\">"
                    + "<a href=\"" + template.getCtaUrl().trim() + "\" style=\"display:inline-block;background:"
                    + ctaBg + ";color:" + ctaFg
                    + ";text-decoration:none;padding:14px 34px;border-radius:999px;font-size:15px;font-weight:700;\">"
                    + escapeHtml(template.getCtaText().trim()) + "</a></td></tr></table>";
        }

        String preheaderHtml = isBlank(preheader) ? ""
                : "<div style=\"display:none;max-height:0;overflow:hidden;opacity:0;color:transparent;\">" + preheader + "</div>";

        String html = """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8"/>
                  <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
                  <title>__TITLE__</title>
                </head>
                <body style="margin:0;padding:0;background-color:#06101c;font-family:Arial,Helvetica,sans-serif;">
                  __PREHEADER__
                  <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="background-color:#06101c;padding:28px 12px;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="600" cellspacing="0" cellpadding="0" style="background:__BODY_COLOR__;border-radius:18px;overflow:hidden;">
                          __HEADER_IMAGES__
                          <tr>
                            <td style="background:__HEADER_COLOR__;padding:32px 36px 28px;text-align:center;">
                              __LOGO__
                              __HEADER_HEADLINE__
                              __HEADER_SUB__
                            </td>
                          </tr>
                          <tr>
                            <td style="background:__BODY_COLOR__;padding:32px 36px 8px;">
                              __GREETING__
                              __BODY_HEADLINE__
                              <div style="color:__BODY_TEXT__;font-size:15px;line-height:1.75;">__BODY_CONTENT__</div>
                            </td>
                          </tr>
                          __CAMPAIGN_IMAGES__
                          __BODY_IMAGES__
                          <tr>
                            <td style="background:__BODY_COLOR__;padding:8px 36px 36px;">
                              __CTA__
                            </td>
                          </tr>
                          <tr>
                            <td style="background:__FOOTER_COLOR__;padding:28px 32px;text-align:center;">
                              __FOOTER_IMAGES__
                              <div style="color:__FOOTER_TEXT__;font-size:13px;line-height:1.7;">__FOOTER_CONTENT__</div>
                              <p style="color:__FOOTER_TEXT__;margin:16px 0 0;font-size:11px;opacity:0.8;">Future Media &middot; Namibia</p>
                              <p style="color:__FOOTER_TEXT__;margin:8px 0 0;font-size:11px;opacity:0.7;">
                                You are receiving this email because you connected with Future Media.
                                <a href="__FRONTEND_URL__" style="color:__FOOTER_TEXT__;text-decoration:underline;">Visit our website</a>
                              </p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """;

        return html
                .replace("__TITLE__", escapeHtml(template.getName()))
                .replace("__PREHEADER__", preheaderHtml)
                .replace("__BODY_COLOR__", bodyColor)
                .replace("__HEADER_COLOR__", headerColor)
                .replace("__LOGO__", logoBlock)
                .replace("__HEADER_IMAGES__", headerImages)
                .replace("__HEADER_HEADLINE__", headerHeadlineHtml)
                .replace("__HEADER_SUB__", headerSubHtml)
                .replace("__GREETING__", greeting)
                .replace("__BODY_IMAGES__", bodyImages)
                .replace("__CAMPAIGN_IMAGES__", campaignGallery)
                .replace("__BODY_HEADLINE__", bodyHeadlineHtml)
                .replace("__BODY_TEXT__", bodyText)
                .replace("__BODY_CONTENT__", isBlank(bodyContent) ? "" : bodyContent)
                .replace("__CTA__", ctaHtml)
                .replace("__FOOTER_COLOR__", footerColor)
                .replace("__FOOTER_IMAGES__", footerImages)
                .replace("__FOOTER_TEXT__", footerText)
                .replace("__FOOTER_CONTENT__", isBlank(footer) ? "Future Media" : footer)
                .replace("__FRONTEND_URL__", frontendUrl != null ? frontendUrl : "#");
    }

    private void applyFields(EmailTemplate template, Map<String, Object> body) {
        if (body.containsKey("preheader")) {
            template.setPreheader(optional(body.get("preheader")));
        }
        if (body.containsKey("headerHeadline")) {
            template.setHeaderHeadline(optional(body.get("headerHeadline")));
        }
        if (body.containsKey("headerSubheadline")) {
            template.setHeaderSubheadline(optional(body.get("headerSubheadline")));
        }
        if (body.containsKey("headerColor")) {
            template.setHeaderColor(normalizeColor(body.get("headerColor"), "headerColor"));
        }
        if (body.containsKey("headerTextColor")) {
            template.setHeaderTextColor(normalizeColor(body.get("headerTextColor"), "headerTextColor"));
        }
        if (body.containsKey("bodyHeadline")) {
            template.setBodyHeadline(optional(body.get("bodyHeadline")));
        }
        if (body.containsKey("bodyContent")) {
            template.setBodyContent(optional(body.get("bodyContent")));
        }
        if (body.containsKey("bodyColor")) {
            template.setBodyColor(normalizeColor(body.get("bodyColor"), "bodyColor"));
        }
        if (body.containsKey("bodyTextColor")) {
            template.setBodyTextColor(normalizeColor(body.get("bodyTextColor"), "bodyTextColor"));
        }
        if (body.containsKey("ctaText")) {
            template.setCtaText(optional(body.get("ctaText")));
        }
        if (body.containsKey("ctaUrl")) {
            template.setCtaUrl(optional(body.get("ctaUrl")));
        }
        if (body.containsKey("ctaButtonColor")) {
            template.setCtaButtonColor(normalizeColor(body.get("ctaButtonColor"), "ctaButtonColor"));
        }
        if (body.containsKey("ctaTextColor")) {
            template.setCtaTextColor(normalizeColor(body.get("ctaTextColor"), "ctaTextColor"));
        }
        if (body.containsKey("footerText")) {
            template.setFooterText(optional(body.get("footerText")));
        }
        if (body.containsKey("footerColor")) {
            template.setFooterColor(normalizeColor(body.get("footerColor"), "footerColor"));
        }
        if (body.containsKey("footerTextColor")) {
            template.setFooterTextColor(normalizeColor(body.get("footerTextColor"), "footerTextColor"));
        }
        if (body.containsKey("useCompanyLogo")) {
            template.setUseCompanyLogo(asBoolean(body.get("useCompanyLogo"), true));
        }
        if (body.containsKey("active")) {
            template.setActive(asBoolean(body.get("active"), true));
        }
        if (template.getHeaderColor() == null) {
            template.setHeaderColor("#0b1f3a");
        }
        if (template.getHeaderTextColor() == null) {
            template.setHeaderTextColor("#ffffff");
        }
        if (template.getBodyColor() == null) {
            template.setBodyColor("#ffffff");
        }
        if (template.getBodyTextColor() == null) {
            template.setBodyTextColor("#334155");
        }
        if (template.getFooterColor() == null) {
            template.setFooterColor("#0b1f3a");
        }
        if (template.getFooterTextColor() == null) {
            template.setFooterTextColor("#a8c0d8");
        }
        if (template.getCtaButtonColor() == null) {
            template.setCtaButtonColor("#1a73e8");
        }
        if (template.getCtaTextColor() == null) {
            template.setCtaTextColor("#ffffff");
        }
    }

    private String headerHeroHtml(EmailTemplate template, String headerColor, String headerText) {
        List<EmailTemplateImage> images = sectionImages(template, TemplateImageSection.HEADER);
        if (images.isEmpty()) {
            return "";
        }
        StringBuilder html = new StringBuilder();
        for (EmailTemplateImage image : images) {
            String url = absUrl(image.getImageUrl());
            String alt = escapeHtml(image.getCaption() != null ? image.getCaption() : "Campaign banner");
            html.append("<tr><td style=\"padding:0;line-height:0;font-size:0;\">")
                    .append("<img src=\"").append(url).append("\" alt=\"").append(alt)
                    .append("\" width=\"600\" style=\"display:block;width:100%;max-width:600px;height:auto;border:0;\"/>")
                    .append("</td></tr>");
            if (image.getCaption() != null && !image.getCaption().isBlank()) {
                html.append("<tr><td style=\"background:").append(headerColor)
                        .append(";padding:10px 24px 14px;text-align:center;\">")
                        .append("<p style=\"margin:0;color:").append(headerText)
                        .append(";font-size:12px;letter-spacing:0.12em;text-transform:uppercase;opacity:0.85;\">")
                        .append(escapeHtml(image.getCaption())).append("</p></td></tr>");
            }
        }
        return html.toString();
    }

    private String bodyGalleryHtml(EmailTemplate template, String bodyText) {
        return photoGalleryHtml(sectionImages(template, TemplateImageSection.BODY), bodyText);
    }

    private String campaignGalleryHtml(List<EmailCampaignImage> campaignImages, String bodyText) {
        if (campaignImages == null || campaignImages.isEmpty()) {
            return "";
        }
        List<EmailTemplateImage> adapted = new ArrayList<>();
        for (EmailCampaignImage image : campaignImages) {
            EmailTemplateImage block = new EmailTemplateImage();
            block.setImageUrl(image.getImageUrl());
            block.setCaption(image.getCaption());
            adapted.add(block);
        }
        return photoGalleryHtml(adapted, bodyText);
    }

    private String photoGalleryHtml(List<EmailTemplateImage> images, String bodyText) {
        if (images == null || images.isEmpty()) {
            return "";
        }
        StringBuilder html = new StringBuilder();
        html.append("<tr><td style=\"padding:8px 36px 12px;\">");
        EmailTemplateImage featured = images.get(0);
        html.append(featuredPhoto(featured, bodyText, true));
        List<EmailTemplateImage> rest = images.subList(1, images.size());
        for (int i = 0; i < rest.size(); i += 2) {
            EmailTemplateImage left = rest.get(i);
            EmailTemplateImage right = i + 1 < rest.size() ? rest.get(i + 1) : null;
            html.append("<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"margin-top:8px;\"><tr>");
            html.append("<td width=\"50%\" valign=\"top\" style=\"padding:0 6px 0 0;\">").append(featuredPhoto(left, bodyText, false)).append("</td>");
            html.append("<td width=\"50%\" valign=\"top\" style=\"padding:0 0 0 6px;\">");
            html.append(right != null ? featuredPhoto(right, bodyText, false) : "&nbsp;");
            html.append("</td></tr></table>");
        }
        html.append("</td></tr>");
        return html.toString();
    }

    private String featuredPhoto(EmailTemplateImage image, String bodyText, boolean large) {
        String url = absUrl(image.getImageUrl());
        String alt = escapeHtml(image.getCaption() != null ? image.getCaption() : "Campaign image");
        String radius = large ? "16px" : "12px";
        StringBuilder html = new StringBuilder();
        html.append("<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"border-collapse:separate;\"><tr><td style=\"padding:0;border-radius:")
                .append(radius).append(";overflow:hidden;background:#f3f5f8;\">")
                .append("<img src=\"").append(url).append("\" alt=\"").append(alt)
                .append("\" width=\"").append(large ? "528" : "256")
                .append("\" style=\"display:block;width:100%;max-width:").append(large ? "528px" : "256px")
                .append(";height:auto;border:0;border-radius:").append(radius).append(";\"/>")
                .append("</td></tr>");
        if (image.getCaption() != null && !image.getCaption().isBlank()) {
            html.append("<tr><td style=\"padding:8px 4px 12px;text-align:center;\">")
                    .append("<p style=\"margin:0;font-size:12px;line-height:1.5;color:").append(bodyText)
                    .append(";opacity:0.72;font-style:italic;\">")
                    .append(escapeHtml(image.getCaption())).append("</p></td></tr>");
        }
        html.append("</table>");
        return html.toString();
    }

    private String footerMarksHtml(EmailTemplate template) {
        List<EmailTemplateImage> images = sectionImages(template, TemplateImageSection.FOOTER);
        if (images.isEmpty()) {
            return "";
        }
        StringBuilder html = new StringBuilder();
        html.append("<table role=\"presentation\" align=\"center\" cellspacing=\"0\" cellpadding=\"0\" style=\"margin:0 auto 16px;\"><tr>");
        for (EmailTemplateImage image : images) {
            String url = absUrl(image.getImageUrl());
            String alt = escapeHtml(image.getCaption() != null ? image.getCaption() : "Future Media");
            html.append("<td style=\"padding:0 8px;\">")
                    .append("<img src=\"").append(url).append("\" alt=\"").append(alt)
                    .append("\" width=\"72\" style=\"display:block;width:72px;height:auto;border:0;border-radius:10px;\"/>")
                    .append("</td>");
        }
        html.append("</tr></table>");
        return html.toString();
    }

    private List<EmailTemplateImage> sectionImages(EmailTemplate template, TemplateImageSection section) {
        List<EmailTemplateImage> matches = new ArrayList<>();
        if (template.getImages() == null) {
            return matches;
        }
        for (EmailTemplateImage image : template.getImages()) {
            if (image.getSection() == section && image.getImageUrl() != null && !image.getImageUrl().isBlank()) {
                matches.add(image);
            }
        }
        return matches;
    }

    private String absUrl(String url) {
        return fileStorageService.toPublicUrl(url);
    }

    private String personalize(String input, String name, String email, String company) {
        if (input == null) {
            return "";
        }
        return input
                .replace("{{name}}", escapeHtml(name != null ? name : ""))
                .replace("{{email}}", escapeHtml(email != null ? email : ""))
                .replace("{{company}}", escapeHtml(company != null ? company : ""));
    }

    private String toHtmlParagraphs(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String trimmed = input.trim();
        if (trimmed.contains("<") && trimmed.contains(">")) {
            return trimmed;
        }
        return escapeHtml(trimmed).replace("\n", "<br/>");
    }

    private String normalizeColor(Object value, String field) {
        if (value == null || value.toString().isBlank()) {
            throw new BadRequestException(field + " is required");
        }
        String color = value.toString().trim();
        if (!color.startsWith("#")) {
            color = "#" + color;
        }
        if (!HEX_COLOR.matcher(color).matches()) {
            throw new BadRequestException(field + " must be a hex color like #0b1f3a");
        }
        return color.toLowerCase(Locale.ROOT);
    }

    private String safeColor(String color, String fallback) {
        if (color == null || !HEX_COLOR.matcher(color).matches()) {
            return fallback;
        }
        return color;
    }

    private TemplateImageSection parseSection(String section) {
        if (section == null || section.isBlank()) {
            throw new BadRequestException("Image section is required (HEADER, BODY or FOOTER)");
        }
        try {
            return TemplateImageSection.valueOf(section.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Image section must be HEADER, BODY or FOOTER");
        }
    }

    private String required(Object value, String message) {
        if (value == null || value.toString().isBlank()) {
            throw new BadRequestException(message);
        }
        return value.toString().trim();
    }

    private String optional(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }

    private boolean asBoolean(Object value, boolean fallback) {
        if (value == null || value.toString().isBlank()) {
            return fallback;
        }
        return Boolean.parseBoolean(value.toString())
                || value.toString().equalsIgnoreCase("yes")
                || value.toString().equals("1");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String firstName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "there";
        }
        String trimmed = fullName.trim();
        int space = trimmed.indexOf(' ');
        return space > 0 ? trimmed.substring(0, space) : trimmed;
    }

    private String escapeHtml(String input) {
        if (input == null) {
            return "";
        }
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
