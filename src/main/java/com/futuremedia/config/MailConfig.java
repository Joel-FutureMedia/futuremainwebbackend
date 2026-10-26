package com.futuremedia.config;

import org.eclipse.angus.mail.util.MailSSLSocketFactory;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.security.GeneralSecurityException;
import java.util.Properties;

/**
 * SimplyFound SMTP uses STARTTLS with a certificate Jakarta Mail does not
 * trust by hostname. Spring {@code ssl.trust} properties are not always
 * applied to the session, so the socket factory is configured here.
 */
@Configuration
@EnableConfigurationProperties(MailProperties.class)
public class MailConfig {

    @Bean
    public JavaMailSender javaMailSender(MailProperties mailProperties) throws GeneralSecurityException {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(mailProperties.getHost());
        sender.setPort(mailProperties.getPort());
        sender.setUsername(mailProperties.getUsername());
        sender.setPassword(mailProperties.getPassword());
        if (mailProperties.getProtocol() != null) {
            sender.setProtocol(mailProperties.getProtocol());
        }
        if (mailProperties.getDefaultEncoding() != null) {
            sender.setDefaultEncoding(mailProperties.getDefaultEncoding().name());
        }

        Properties props = sender.getJavaMailProperties();
        props.putAll(mailProperties.getProperties());

        MailSSLSocketFactory socketFactory = new MailSSLSocketFactory();
        socketFactory.setTrustAllHosts(true);
        props.put("mail.smtp.ssl.socketFactory", socketFactory);
        props.put("mail.smtp.ssl.trust", "*");
        props.put("mail.smtp.ssl.checkserveridentity", "false");
        props.put("mail.smtp.ssl.enable", "false");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");
        props.put("mail.smtp.connectiontimeout", "8000");
        props.put("mail.smtp.timeout", "8000");
        props.put("mail.smtp.writetimeout", "8000");

        return sender;
    }
}
