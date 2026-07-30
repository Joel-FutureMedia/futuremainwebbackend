package com.futuremedia.services;

import com.futuremedia.models.*;
import com.futuremedia.repositories.SubscriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

@Service
public class SubscriptionService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private EmailService emailService;

    public Subscription subscribe(String fullName, String email, String language) {
        if (fullName == null || fullName.isBlank()) {
            throw new BadRequestException("Full name is required");
        }
        if (email == null || email.isBlank() || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new BadRequestException("Valid email is required");
        }
        if (language == null || language.isBlank()) {
            throw new BadRequestException("Language is required. Choose ENGLISH or AFRIKAANS");
        }

        Language lang;
        try {
            lang = Language.valueOf(language.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid language. Choose ENGLISH or AFRIKAANS");
        }

        String normalizedEmail = email.trim().toLowerCase();
        if (subscriptionRepository.existsByEmail(normalizedEmail)) {
            throw new BadRequestException("This email is already subscribed");
        }

        Subscription subscription = new Subscription();
        subscription.setFullName(fullName.trim());
        subscription.setEmail(normalizedEmail);
        subscription.setLanguage(lang);
        subscription.setActive(true);

        Subscription saved = subscriptionRepository.save(subscription);
        // Welcome email is sent asynchronously
        emailService.sendWelcomeSubscriptionEmail(saved);
        return saved;
    }

    public List<Subscription> getAll() {
        return subscriptionRepository.findAllByOrderBySubscribedDateDesc();
    }

    public List<Subscription> getActive() {
        return subscriptionRepository.findByActiveTrue();
    }

    public Subscription getById(Long id) {
        return subscriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found with id: " + id));
    }

    public Subscription unsubscribe(Long id) {
        Subscription subscription = getById(id);
        subscription.setActive(false);
        return subscriptionRepository.save(subscription);
    }

    public void delete(Long id) {
        Subscription subscription = getById(id);
        subscriptionRepository.delete(subscription);
    }
}
