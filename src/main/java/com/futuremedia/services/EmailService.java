package com.futuremedia.services;

import com.futuremedia.models.ContactUs;
import com.futuremedia.models.GatedDocument;
import com.futuremedia.models.GatedDocumentRequest;
import com.futuremedia.models.Language;
import com.futuremedia.models.MediaAudit;
import com.futuremedia.models.News;
import com.futuremedia.models.ShopbeatDemoRequest;
import com.futuremedia.models.Subscription;
import com.futuremedia.repositories.ContactUsRepository;
import com.futuremedia.repositories.GatedDocumentRequestRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private GatedDocumentRequestRepository gatedDocumentRequestRepository;

    @Autowired
    private ContactUsRepository contactUsRepository;

    @Value("${spring.mail.from}")
    private String fromEmail;

    @Value("${app.contact.recipient}")
    private String contactRecipient;

    @Value("${app.logo.url}")
    private String logoUrl;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Value("${server.port}")
    private String serverPort;

    @Async("emailTaskExecutor")
    public void sendContactUsEmail(ContactUs contact) {
        try {
            String html = buildContactUsEmailHtml(contact);
            sendHtmlEmail(contactRecipient, "New Contact Form Submission — " + contact.getName(), html);
            contactUsRepository.findById(contact.getId()).ifPresent(saved -> {
                saved.setEmailSent(true);
                contactUsRepository.save(saved);
            });
        } catch (Exception e) {
            logMailFailure("contact form", contactRecipient, e);
        }
    }

    @Async("emailTaskExecutor")
    public void sendGatedDocumentEmail(GatedDocumentRequest request, GatedDocument document, String downloadUrl) {
        try {
            String html = buildGatedDocumentEmailHtml(request, document, downloadUrl);
            sendHtmlEmail(request.getEmail(), "Your document from Future Media: " + document.getTitle(), html);
            gatedDocumentRequestRepository.findById(request.getId()).ifPresent(saved -> {
                saved.setEmailSent(true);
                gatedDocumentRequestRepository.save(saved);
            });
        } catch (Exception e) {
            logMailFailure("gated document", request.getEmail(), e);
        }
    }

    @Async("emailTaskExecutor")
    public void sendMediaAuditEmail(MediaAudit audit) {
        try {
            sendHtmlEmail(contactRecipient, "New Media Audit Request — " + audit.getCampaignName(),
                    buildMediaAuditEmailHtml(audit));
        } catch (Exception e) {
            logMailFailure("media audit", contactRecipient, e);
        }
    }

    @Async("emailTaskExecutor")
    public void sendShopbeatDemoEmail(ShopbeatDemoRequest request) {
        try {
            sendHtmlEmail(contactRecipient, "New Shopbeat Demo Request — " + request.getName(),
                    buildShopbeatDemoEmailHtml(request));
        } catch (Exception e) {
            logMailFailure("shopbeat demo", contactRecipient, e);
        }
    }

    @Async("emailTaskExecutor")
    public void sendWelcomeSubscriptionEmail(Subscription subscription) {
        try {
            String html = buildWelcomeEmailHtml(subscription);
            String subject = subscription.getLanguage() == Language.AFRIKAANS
                    ? "Welkom by Future Media Nuusbrief"
                    : "Welcome to Future Media Newsletter";
            sendHtmlEmail(subscription.getEmail(), subject, html);
        } catch (Exception e) {
            logMailFailure("welcome subscription", subscription.getEmail(), e);
        }
    }

    public void sendNewsNewsletter(Subscription subscription, News news, String mediaAbsoluteUrl) {
        String html = buildNewsletterEmailHtml(subscription, news, mediaAbsoluteUrl);
        String subject = subscription.getLanguage() == Language.AFRIKAANS
                ? "Nuusbrief: " + news.getTitle()
                : "Newsletter: " + news.getTitle();
        sendHtmlEmail(subscription.getEmail(), subject, html);
    }

    /**
     * Bulk email marketing — runs asynchronously so API responses are not blocked
     * while newsletters are sent to all active subscribers.
     */
    @Async("emailTaskExecutor")
    public void sendBulkNewsletter(List<Subscription> subscribers, News news, String mediaAbsoluteUrl) {
        if (subscribers == null || subscribers.isEmpty()) {
            System.out.println("No active subscribers for newsletter campaign.");
            return;
        }

        System.out.println("Starting async bulk newsletter for news id=" + news.getId()
                + " to " + subscribers.size() + " subscribers...");

        int sent = 0;
        int failed = 0;

        for (Subscription subscriber : subscribers) {
            try {
                sendNewsNewsletter(subscriber, news, mediaAbsoluteUrl);
                sent++;
            } catch (Exception e) {
                failed++;
                System.err.println("Failed to send newsletter to " + subscriber.getEmail() + ": " + e.getMessage());
            }
        }

        System.out.println("Bulk newsletter completed. Sent=" + sent + ", Failed=" + failed);
    }

    public void sendMarketingEmail(String to, String subject, String htmlBody) {
        sendHtmlEmail(to, subject, htmlBody);
    }

    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, "Future Media");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            System.out.println("Email sent to " + to + " — " + subject);
        } catch (MessagingException | MailException | java.io.UnsupportedEncodingException e) {
            throw new RuntimeException("Failed to send email: " + rootMailMessage(e), e);
        }
    }

    private void logMailFailure(String kind, String to, Exception e) {
        System.err.println("Failed to send " + kind + " email to " + to + ": " + rootMailMessage(e));
        e.printStackTrace();
    }

    private String rootMailMessage(Throwable e) {
        Throwable current = e;
        String message = e.getMessage();
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
            if (current.getMessage() != null && !current.getMessage().isBlank()) {
                message = current.getMessage();
            }
        }
        return message;
    }

    private String buildContactUsEmailHtml(ContactUs contact) {
        return wrapAdminEmail(
                "New Contact Message",
                "Someone reached out via the website",
                row("Name", escapeHtml(contact.getName()))
                        + row("Email", "<a href=\"mailto:" + escapeHtml(contact.getEmail()) + "\" style=\"color:#1a73e8;text-decoration:none;\">" + escapeHtml(contact.getEmail()) + "</a>")
                        + row("Telephone", escapeHtml(contact.getTelephone()))
                        + row("Company", escapeHtml(contact.getCompany()))
                        + row("Brand", escapeHtml(contact.getBrand() != null ? contact.getBrand().getName() : "N/A"))
                        + row("Sales", escapeHtml(contact.getSales() != null ? contact.getSales().getName() : "N/A"))
                        + row("Service", escapeHtml(contact.getService() != null ? contact.getService().getName() : "N/A"))
                        + row("Submitted", contact.getSubmittedDate() != null ? contact.getSubmittedDate().toString() : ""),
                "This email was sent automatically from the Contact Us form."
        );
    }

    private String buildGatedDocumentEmailHtml(GatedDocumentRequest request, GatedDocument document, String downloadUrl) {
        String firstName = firstName(request.getName());
        String description = document.getDescription() != null && !document.getDescription().isBlank()
                ? "<p style=\"color:#475569;font-size:15px;line-height:1.7;margin:0 0 24px;\">" + escapeHtml(document.getDescription()).replace("\n", "<br/>") + "</p>"
                : "";
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8"/>
                  <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
                  <title>Your Document</title>
                </head>
                <body style="margin:0;padding:0;background-color:#f4f6f8;font-family:Arial,Helvetica,sans-serif;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background-color:#f4f6f8;padding:40px 16px;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="600" cellspacing="0" cellpadding="0" style="background:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.08);">
                          <tr>
                            <td style="background:linear-gradient(135deg,#0b1f3a 0%%,#1a3a5c 100%%);padding:32px;text-align:center;">
                              <img src="%s" alt="Future Media" style="max-width:180px;height:auto;margin-bottom:16px;"/>
                              <h1 style="color:#ffffff;margin:0;font-size:22px;font-weight:600;">Your Document is Ready</h1>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:40px 32px;">
                              <h2 style="color:#0b1f3a;margin:0 0 12px;font-size:22px;">Hi %s</h2>
                              <p style="color:#475569;font-size:15px;line-height:1.7;margin:0 0 16px;">
                                Thank you for your interest in Future Media. As requested, here is your document:
                              </p>
                              <p style="color:#0b1f3a;font-size:18px;font-weight:700;margin:0 0 16px;">%s</p>
                              %s
                              <p style="text-align:center;margin:32px 0;">
                                <a href="%s" style="display:inline-block;background:#1a73e8;color:#ffffff;text-decoration:none;padding:14px 36px;border-radius:8px;font-size:16px;font-weight:700;">Download</a>
                              </p>
                              <p style="color:#64748b;font-size:13px;line-height:1.6;margin:0;">
                                If the button does not work, copy and paste this link into your browser:<br/>
                                <a href="%s" style="color:#1a73e8;word-break:break-all;">%s</a>
                              </p>
                            </td>
                          </tr>
                          <tr>
                            <td style="background:#0b1f3a;padding:24px;text-align:center;">
                              <img src="%s" alt="Future Media" style="max-width:100px;height:auto;opacity:0.7;margin-bottom:12px;"/>
                              <p style="color:#a8c0d8;margin:0;font-size:12px;">Future Media &middot; Namibia</p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(
                logoUrl,
                escapeHtml(firstName),
                escapeHtml(document.getTitle()),
                description,
                downloadUrl,
                downloadUrl,
                downloadUrl,
                logoUrl
        );
    }

    private String buildMediaAuditEmailHtml(MediaAudit audit) {
        String campaignType = audit.getCampaignType();
        if ("Other".equals(campaignType) && audit.getCampaignTypeOther() != null && !audit.getCampaignTypeOther().isBlank()) {
            campaignType = "Other — " + audit.getCampaignTypeOther();
        }
        return wrapAdminEmail(
                "New Media Audit Request",
                "A client requested a media audit",
                row("Name", escapeHtml(audit.getName()))
                        + row("Email", "<a href=\"mailto:" + escapeHtml(audit.getEmail()) + "\" style=\"color:#1a73e8;text-decoration:none;\">" + escapeHtml(audit.getEmail()) + "</a>")
                        + row("Telephone", escapeHtml(audit.getTelephone()))
                        + row("Company", escapeHtml(audit.getCompany()))
                        + row("Campaign Name", escapeHtml(audit.getCampaignName()))
                        + row("Location", escapeHtml(displayValue(audit.getLocation())))
                        + row("Business description", escapeHtml(displayValue(audit.getBusinessDescription())))
                        + row("Campaign type", escapeHtml(displayValue(campaignType)))
                        + row("Primary audience", escapeHtml(displayValue(audit.getPrimaryAudience())))
                        + row("Current advertising", escapeHtml(displayValue(audit.getCurrentAdvertising())))
                        + row("When they want to advertise", escapeHtml(displayValue(audit.getAdvertiseTimeline())))
                        + row("Advertising budget", escapeHtml(displayValue(audit.getAdvertisingBudget())))
                        + row("Additional Details", escapeHtml(displayValue(audit.getAdditionalDetails())).replace("\n", "<br/>"))
                        + row("Submitted", audit.getSubmittedDate() != null ? audit.getSubmittedDate().toString() : ""),
                "This email was sent automatically from the Media Audit form."
        );
    }

    private String displayValue(String value) {
        return value == null || value.isBlank() ? "N/A" : value;
    }

    private String buildShopbeatDemoEmailHtml(ShopbeatDemoRequest request) {
        String inspiration = request.isHasSoundInspiration()
                ? escapeHtml(request.getSoundInspiration() != null ? request.getSoundInspiration() : "").replace("\n", "<br/>")
                : "No";
        return wrapAdminEmail(
                "New Shopbeat Demo Request",
                "A client requested a Shopbeat demo",
                row("Name", escapeHtml(request.getName()))
                        + row("Email", "<a href=\"mailto:" + escapeHtml(request.getEmail()) + "\" style=\"color:#1a73e8;text-decoration:none;\">" + escapeHtml(request.getEmail()) + "</a>")
                        + row("Telephone", escapeHtml(request.getTelephone()))
                        + row("Company", escapeHtml(request.getCompany()))
                        + row("Feeling", escapeHtml(request.getFeeling() != null ? request.getFeeling().getName() : "N/A"))
                        + row("Sound Inspiration", request.isHasSoundInspiration() ? "Yes" : "No")
                        + row("Inspiration Details", inspiration)
                        + row("Message", escapeHtml(request.getMessage()).replace("\n", "<br/>"))
                        + row("Submitted", request.getSubmittedDate() != null ? request.getSubmittedDate().toString() : ""),
                "This email was sent automatically from the Shopbeat demo form."
        );
    }

    private String wrapAdminEmail(String title, String subtitle, String rows, String footerNote) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8"/>
                  <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
                  <title>%s</title>
                </head>
                <body style="margin:0;padding:0;background-color:#f4f6f8;font-family:Arial,Helvetica,sans-serif;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background-color:#f4f6f8;padding:40px 16px;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="600" cellspacing="0" cellpadding="0" style="background:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.08);">
                          <tr>
                            <td style="background:linear-gradient(135deg,#0b1f3a 0%%,#1a3a5c 100%%);padding:32px;text-align:center;">
                              <img src="%s" alt="Future Media" style="max-width:180px;height:auto;margin-bottom:16px;"/>
                              <h1 style="color:#ffffff;margin:0;font-size:22px;font-weight:600;">%s</h1>
                              <p style="color:#a8c0d8;margin:8px 0 0;font-size:14px;">%s</p>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:32px;">
                              <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="border-collapse:collapse;">
                                %s
                              </table>
                            </td>
                          </tr>
                          <tr>
                            <td style="background:#0b1f3a;padding:24px;text-align:center;">
                              <p style="color:#a8c0d8;margin:0;font-size:12px;">Future Media Admin Portal</p>
                              <p style="color:#64748b;margin:8px 0 0;font-size:11px;">%s</p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(title, logoUrl, title, subtitle, rows, footerNote);
    }

    private String row(String label, String value) {
        return """
                <tr>
                  <td style="padding:12px 16px;background:#f8fafc;border-bottom:1px solid #e2e8f0;width:35%%;font-weight:600;color:#334155;font-size:14px;vertical-align:top;">%s</td>
                  <td style="padding:12px 16px;background:#ffffff;border-bottom:1px solid #e2e8f0;color:#0f172a;font-size:14px;line-height:1.6;">%s</td>
                </tr>
                """.formatted(label, value);
    }

    private String buildWelcomeEmailHtml(Subscription subscription) {
        boolean afrikaans = subscription.getLanguage() == Language.AFRIKAANS;
        String greeting = afrikaans
                ? "Hallo " + escapeHtml(subscription.getFullName()) + "!"
                : "Hello " + escapeHtml(subscription.getFullName()) + "!";
        String headline = afrikaans ? "Welkom by Future Media" : "Welcome to Future Media";
        String body = afrikaans
                ? "Dankie dat jy vir ons nuusbrief ingeteken het. Jy sal die nuutste nuus, gebeurtenisse en opdaterings direk in jou posbus ontvang."
                : "Thank you for subscribing to our newsletter. You will receive the latest news, events and updates straight to your inbox.";
        String cta = afrikaans ? "Besoek ons Webwerf" : "Visit Our Website";

        return """
                <!DOCTYPE html>
                <html lang="%s">
                <head>
                  <meta charset="UTF-8"/>
                  <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
                  <title>%s</title>
                </head>
                <body style="margin:0;padding:0;background-color:#f4f6f8;font-family:Arial,Helvetica,sans-serif;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background-color:#f4f6f8;padding:40px 16px;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="600" cellspacing="0" cellpadding="0" style="background:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.08);">
                          <tr>
                            <td style="background:linear-gradient(135deg,#0b1f3a 0%%,#1a3a5c 100%%);padding:40px 32px;text-align:center;">
                              <img src="%s" alt="Future Media" style="max-width:180px;height:auto;margin-bottom:20px;"/>
                              <h1 style="color:#ffffff;margin:0;font-size:26px;font-weight:700;">%s</h1>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:40px 32px;text-align:center;">
                              <h2 style="color:#0b1f3a;margin:0 0 16px;font-size:20px;">%s</h2>
                              <p style="color:#475569;font-size:15px;line-height:1.7;margin:0 0 28px;">%s</p>
                              <a href="%s" style="display:inline-block;background:#1a73e8;color:#ffffff;text-decoration:none;padding:14px 32px;border-radius:8px;font-size:15px;font-weight:600;">%s</a>
                            </td>
                          </tr>
                          <tr>
                            <td style="background:#0b1f3a;padding:24px;text-align:center;">
                              <img src="%s" alt="Future Media" style="max-width:100px;height:auto;opacity:0.7;margin-bottom:12px;"/>
                              <p style="color:#a8c0d8;margin:0;font-size:12px;">Future Media &middot; Namibia</p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(
                afrikaans ? "af" : "en",
                headline,
                logoUrl,
                headline,
                greeting,
                body,
                frontendUrl,
                cta,
                logoUrl
        );
    }

    private String buildNewsletterEmailHtml(Subscription subscription, News news, String mediaAbsoluteUrl) {
        boolean afrikaans = subscription.getLanguage() == Language.AFRIKAANS;
        String greeting = afrikaans
                ? "Hallo " + escapeHtml(subscription.getFullName()) + ","
                : "Hello " + escapeHtml(subscription.getFullName()) + ",";
        String intro = afrikaans
                ? "Hier is die nuutste nuus van Future Media:"
                : "Here is the latest news from Future Media:";
        String readMore = afrikaans ? "Lees Meer" : "Read More";
        String mediaBlock = "";
        if (mediaAbsoluteUrl != null && "IMAGE".equalsIgnoreCase(news.getMediaType())) {
            mediaBlock = "<img src=\"" + mediaAbsoluteUrl + "\" alt=\"" + escapeHtml(news.getTitle())
                    + "\" style=\"width:100%;max-height:320px;object-fit:cover;border-radius:8px;margin-bottom:24px;display:block;\"/>";
        }

        return """
                <!DOCTYPE html>
                <html lang="%s">
                <head>
                  <meta charset="UTF-8"/>
                  <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
                  <title>%s</title>
                </head>
                <body style="margin:0;padding:0;background-color:#f4f6f8;font-family:Arial,Helvetica,sans-serif;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background-color:#f4f6f8;padding:40px 16px;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="600" cellspacing="0" cellpadding="0" style="background:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.08);">
                          <tr>
                            <td style="background:linear-gradient(135deg,#0b1f3a 0%%,#1a3a5c 100%%);padding:28px 32px;text-align:center;">
                              <img src="%s" alt="Future Media" style="max-width:160px;height:auto;"/>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:32px;">
                              <p style="color:#64748b;font-size:14px;margin:0 0 8px;">%s</p>
                              <p style="color:#475569;font-size:14px;margin:0 0 24px;">%s</p>
                              %s
                              <h1 style="color:#0b1f3a;margin:0 0 16px;font-size:24px;line-height:1.3;">%s</h1>
                              <p style="color:#334155;font-size:15px;line-height:1.7;margin:0 0 28px;">%s</p>
                              <a href="%s/news/%s" style="display:inline-block;background:#1a73e8;color:#ffffff;text-decoration:none;padding:14px 32px;border-radius:8px;font-size:15px;font-weight:600;">%s</a>
                            </td>
                          </tr>
                          <tr>
                            <td style="background:#0b1f3a;padding:24px;text-align:center;">
                              <p style="color:#a8c0d8;margin:0;font-size:12px;">Future Media Newsletter</p>
                              <p style="color:#64748b;margin:8px 0 0;font-size:11px;">You are receiving this because you subscribed to our newsletter.</p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(
                afrikaans ? "af" : "en",
                escapeHtml(news.getTitle()),
                logoUrl,
                greeting,
                intro,
                mediaBlock,
                escapeHtml(news.getTitle()),
                escapeHtml(news.getDescription()).replace("\n", "<br/>"),
                frontendUrl,
                news.getId(),
                readMore
        );
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
