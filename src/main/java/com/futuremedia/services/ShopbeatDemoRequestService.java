package com.futuremedia.services;

import com.futuremedia.models.BadRequestException;
import com.futuremedia.models.ResourceNotFoundException;
import com.futuremedia.models.ShopbeatDemoRequest;
import com.futuremedia.models.ShopbeatFeeling;
import com.futuremedia.repositories.ShopbeatDemoRequestRepository;
import com.futuremedia.repositories.ShopbeatFeelingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

@Service
public class ShopbeatDemoRequestService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    @Autowired
    private ShopbeatDemoRequestRepository shopbeatDemoRequestRepository;

    @Autowired
    private ShopbeatFeelingRepository shopbeatFeelingRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private LeadService leadService;

    public List<ShopbeatFeeling> getActiveFeelings() {
        return shopbeatFeelingRepository.findByActiveTrueOrderByNameAsc();
    }

    public List<ShopbeatFeeling> getAllFeelings() {
        return shopbeatFeelingRepository.findAll();
    }

    public ShopbeatFeeling getFeelingById(Long id) {
        return shopbeatFeelingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feeling not found with id: " + id));
    }

    public void deleteFeeling(Long id) {
        ShopbeatFeeling feeling = getFeelingById(id);
        shopbeatFeelingRepository.delete(feeling);
    }

    public ShopbeatFeeling createFeeling(String name) {
        if (name == null || name.isBlank()) {
            throw new BadRequestException("Feeling name is required");
        }
        if (shopbeatFeelingRepository.existsByNameIgnoreCase(name.trim())) {
            throw new BadRequestException("A feeling with this name already exists");
        }
        ShopbeatFeeling feeling = new ShopbeatFeeling();
        feeling.setName(name.trim());
        feeling.setActive(true);
        return shopbeatFeelingRepository.save(feeling);
    }

    public ShopbeatFeeling updateFeeling(Long id, String name, Boolean active) {
        ShopbeatFeeling feeling = shopbeatFeelingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feeling not found with id: " + id));
        if (name != null && !name.isBlank()) {
            shopbeatFeelingRepository.findByNameIgnoreCase(name.trim()).ifPresent(existing -> {
                if (!existing.getId().equals(id)) {
                    throw new BadRequestException("A feeling with this name already exists");
                }
            });
            feeling.setName(name.trim());
        }
        if (active != null) {
            feeling.setActive(active);
        }
        return shopbeatFeelingRepository.save(feeling);
    }

    public ShopbeatDemoRequest submit(String name, String email, String telephone, String company,
                                      Long feelingId, Boolean hasSoundInspiration,
                                      String soundInspiration, String message) {
        if (name == null || name.isBlank()) {
            throw new BadRequestException("Name is required");
        }
        if (email == null || email.isBlank() || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new BadRequestException("Valid email is required");
        }
        if (telephone == null || telephone.isBlank()) {
            throw new BadRequestException("Telephone is required");
        }
        if (company == null || company.isBlank()) {
            throw new BadRequestException("Company you represent is required");
        }
        if (feelingId == null) {
            throw new BadRequestException("Please select the feeling you want to create");
        }
        if (hasSoundInspiration == null) {
            throw new BadRequestException("Please indicate whether you have a specific sound inspiration");
        }
        if (hasSoundInspiration && (soundInspiration == null || soundInspiration.isBlank())) {
            throw new BadRequestException("Please tell us about your sound inspiration");
        }
        if (message == null || message.isBlank()) {
            throw new BadRequestException("Message is required");
        }

        ShopbeatFeeling feeling = shopbeatFeelingRepository.findById(feelingId)
                .orElseThrow(() -> new ResourceNotFoundException("Feeling not found with id: " + feelingId));
        if (!feeling.isActive()) {
            throw new BadRequestException("Selected feeling is not available");
        }

        ShopbeatDemoRequest request = new ShopbeatDemoRequest();
        request.setName(name.trim());
        request.setEmail(email.trim().toLowerCase());
        request.setTelephone(telephone.trim());
        request.setCompany(company.trim());
        request.setFeeling(feeling);
        request.setHasSoundInspiration(hasSoundInspiration);
        request.setSoundInspiration(hasSoundInspiration ? soundInspiration.trim() : null);
        request.setMessage(message.trim());
        ShopbeatDemoRequest saved = shopbeatDemoRequestRepository.save(request);

        leadService.captureQuietly(saved.getName(), saved.getEmail(), saved.getTelephone(),
                saved.getCompany(), "Shopbeat");
        emailService.sendShopbeatDemoEmail(saved);
        return saved;
    }

    public List<ShopbeatDemoRequest> getAll() {
        return shopbeatDemoRequestRepository.findAllByOrderBySubmittedDateDesc();
    }

    public ShopbeatDemoRequest getById(Long id) {
        return shopbeatDemoRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shopbeat demo request not found with id: " + id));
    }

    public void delete(Long id) {
        ShopbeatDemoRequest request = getById(id);
        shopbeatDemoRequestRepository.delete(request);
    }
}
