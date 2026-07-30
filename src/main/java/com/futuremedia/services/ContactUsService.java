package com.futuremedia.services;

import com.futuremedia.models.BadRequestException;
import com.futuremedia.models.ContactUs;
import com.futuremedia.models.ResourceNotFoundException;
import com.futuremedia.repositories.ContactUsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

@Service
public class ContactUsService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    @Autowired
    private ContactUsRepository contactUsRepository;

    @Autowired
    private EmailService emailService;

    @Transactional
    public ContactUs submit(String fullName, String email, String phone, String subject, String message) {
        if (fullName == null || fullName.isBlank()) {
            throw new BadRequestException("Full name is required");
        }
        if (email == null || email.isBlank() || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new BadRequestException("Valid email is required");
        }
        if (subject == null || subject.isBlank()) {
            throw new BadRequestException("Subject is required");
        }
        if (message == null || message.isBlank()) {
            throw new BadRequestException("Message is required");
        }

        ContactUs contact = new ContactUs();
        contact.setFullName(fullName.trim());
        contact.setEmail(email.trim().toLowerCase());
        contact.setPhone(phone != null ? phone.trim() : null);
        contact.setSubject(subject.trim());
        contact.setMessage(message.trim());
        contact.setEmailSent(false);

        return contactUsRepository.save(contact);
    }

    public ContactUs submitAndNotify(String fullName, String email, String phone, String subject, String message) {
        ContactUs saved = submit(fullName, email, phone, subject, message);
        try {
            emailService.sendContactUsEmail(saved);
            markEmailSent(saved.getId());
        } catch (Exception e) {
            System.err.println("Contact form saved but email failed: " + e.getMessage());
        }
        return saved;
    }

    @Transactional
    public void markEmailSent(Long id) {
        contactUsRepository.findById(id).ifPresent(c -> {
            c.setEmailSent(true);
            contactUsRepository.save(c);
        });
    }

    public List<ContactUs> getAll() {
        return contactUsRepository.findAllByOrderBySubmittedDateDesc();
    }

    public ContactUs getById(Long id) {
        return contactUsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contact submission not found with id: " + id));
    }

    public void delete(Long id) {
        ContactUs contact = getById(id);
        contactUsRepository.delete(contact);
    }
}
