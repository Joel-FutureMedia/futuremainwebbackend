package com.futuremedia.services;

import com.futuremedia.models.ContactUs;
import com.futuremedia.models.Language;
import com.futuremedia.models.News;
import com.futuremedia.models.Subscription;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

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

    public void sendContactUsEmail(ContactUs contact) {
        String html = buildContactUsEmailHtml(contact);
        sendHtmlEmail(contactRecipient, "New Contact Form Submission — " + contact.getSubject(), html);
    }

    @Async("emailTaskExecutor")
    public void sendWelcomeSubscriptionEmail(Subscription subscription) {
        String html = buildWelcomeEmailHtml(subscription);
        String subject = subscription.getLanguage() == Language.AFRIKAANS
                ? "Welkom by Future Media Nuusbrief"
                : "Welcome to Future Media Newsletter";
        sendHtmlEmail(subscription.getEmail(), subject, html);
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

    private void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        }
    }

    private String buildContactUsEmailHtml(ContactUs contact) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8"/>
                  <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
                  <title>Contact Form Submission</title>
                </head>
                <body style="margin:0;padding:0;background-color:#f4f6f8;font-family:Arial,Helvetica,sans-serif;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background-color:#f4f6f8;padding:40px 16px;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="600" cellspacing="0" cellpadding="0" style="background:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.08);">
                          <tr>
                            <td style="background:linear-gradient(135deg,#0b1f3a 0%%,#1a3a5c 100%%);padding:32px;text-align:center;">
                              <img src="%s" alt="Future Media" style="max-width:180px;height:auto;margin-bottom:16px;"/>
                              <h1 style="color:#ffffff;margin:0;font-size:22px;font-weight:600;">New Contact Message</h1>
                              <p style="color:#a8c0d8;margin:8px 0 0;font-size:14px;">Someone reached out via the website</p>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:32px;">
                              <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="border-collapse:collapse;">
                                <tr>
                                  <td style="padding:12px 16px;background:#f8fafc;border-bottom:1px solid #e2e8f0;width:35%%;font-weight:600;color:#334155;font-size:14px;">Full Name</td>
                                  <td style="padding:12px 16px;background:#ffffff;border-bottom:1px solid #e2e8f0;color:#0f172a;font-size:14px;">%s</td>
                                </tr>
                                <tr>
                                  <td style="padding:12px 16px;background:#f8fafc;border-bottom:1px solid #e2e8f0;font-weight:600;color:#334155;font-size:14px;">Email</td>
                                  <td style="padding:12px 16px;background:#ffffff;border-bottom:1px solid #e2e8f0;color:#0f172a;font-size:14px;"><a href="mailto:%s" style="color:#1a73e8;text-decoration:none;">%s</a></td>
                                </tr>
                                <tr>
                                  <td style="padding:12px 16px;background:#f8fafc;border-bottom:1px solid #e2e8f0;font-weight:600;color:#334155;font-size:14px;">Phone</td>
                                  <td style="padding:12px 16px;background:#ffffff;border-bottom:1px solid #e2e8f0;color:#0f172a;font-size:14px;">%s</td>
                                </tr>
                                <tr>
                                  <td style="padding:12px 16px;background:#f8fafc;border-bottom:1px solid #e2e8f0;font-weight:600;color:#334155;font-size:14px;">Subject</td>
                                  <td style="padding:12px 16px;background:#ffffff;border-bottom:1px solid #e2e8f0;color:#0f172a;font-size:14px;">%s</td>
                                </tr>
                                <tr>
                                  <td style="padding:12px 16px;background:#f8fafc;border-bottom:1px solid #e2e8f0;font-weight:600;color:#334155;font-size:14px;vertical-align:top;">Message</td>
                                  <td style="padding:12px 16px;background:#ffffff;border-bottom:1px solid #e2e8f0;color:#0f172a;font-size:14px;line-height:1.6;">%s</td>
                                </tr>
                                <tr>
                                  <td style="padding:12px 16px;background:#f8fafc;font-weight:600;color:#334155;font-size:14px;">Submitted</td>
                                  <td style="padding:12px 16px;background:#ffffff;color:#0f172a;font-size:14px;">%s</td>
                                </tr>
                              </table>
                            </td>
                          </tr>
                          <tr>
                            <td style="background:#0b1f3a;padding:24px;text-align:center;">
                              <p style="color:#a8c0d8;margin:0;font-size:12px;">Future Media Admin Portal</p>
                              <p style="color:#64748b;margin:8px 0 0;font-size:11px;">This email was sent automatically from the Contact Us form.</p>
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
                escapeHtml(contact.getFullName()),
                escapeHtml(contact.getEmail()),
                escapeHtml(contact.getEmail()),
                escapeHtml(contact.getPhone() != null ? contact.getPhone() : "N/A"),
                escapeHtml(contact.getSubject()),
                escapeHtml(contact.getMessage()).replace("\n", "<br/>"),
                contact.getSubmittedDate() != null ? contact.getSubmittedDate().toString() : ""
        );
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
