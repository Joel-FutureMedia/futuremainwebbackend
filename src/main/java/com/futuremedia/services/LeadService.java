package com.futuremedia.services;

import com.futuremedia.models.BadRequestException;
import com.futuremedia.models.Lead;
import com.futuremedia.models.LeadSource;
import com.futuremedia.models.ResourceNotFoundException;
import com.futuremedia.repositories.LeadRepository;
import com.futuremedia.repositories.LeadSourceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

@Service
public class LeadService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    @Autowired
    private LeadRepository leadRepository;

    @Autowired
    private LeadSourceRepository leadSourceRepository;

    public Lead capture(String name, String email, String telephone, String company, Long sourceId, String sourceName) {
        if (name == null || name.isBlank()) {
            throw new BadRequestException("Name is required");
        }
        if (email == null || email.isBlank() || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new BadRequestException("Valid email is required");
        }
        if (telephone == null || telephone.isBlank()) {
            throw new BadRequestException("Telephone is required");
        }

        LeadSource source = resolveSource(sourceId, sourceName);

        Lead lead = new Lead();
        lead.setName(name.trim());
        lead.setEmail(email.trim().toLowerCase());
        lead.setTelephone(telephone.trim());
        lead.setCompany(company != null ? company.trim() : null);
        lead.setSource(source);
        return leadRepository.save(lead);
    }

    public void captureQuietly(String name, String email, String telephone, String company, String sourceName) {
        try {
            capture(name, email, telephone, company, null, sourceName);
        } catch (Exception e) {
            System.err.println("Lead capture failed for source " + sourceName + ": " + e.getMessage());
        }
    }

    public List<Lead> getAll() {
        return leadRepository.findAllByOrderByCapturedDateDesc();
    }

    public List<Lead> getBySource(Long sourceId) {
        LeadSource source = leadSourceRepository.findById(sourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead source not found with id: " + sourceId));
        return leadRepository.findBySourceOrderByCapturedDateDesc(source);
    }

    public List<Lead> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return getAll();
        }
        return leadRepository.search(keyword.trim());
    }

    public Lead getById(Long id) {
        return leadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found with id: " + id));
    }

    public void delete(Long id) {
        Lead lead = getById(id);
        leadRepository.delete(lead);
    }

    public List<LeadSource> getActiveSources() {
        return leadSourceRepository.findByActiveTrueOrderByNameAsc();
    }

    public List<LeadSource> getAllSources() {
        return leadSourceRepository.findAll();
    }

    public LeadSource getSourceById(Long id) {
        return leadSourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lead source not found with id: " + id));
    }

    public LeadSource createSource(String name) {
        if (name == null || name.isBlank()) {
            throw new BadRequestException("Source name is required");
        }
        if (leadSourceRepository.existsByNameIgnoreCase(name.trim())) {
            throw new BadRequestException("A lead source with this name already exists");
        }
        LeadSource source = new LeadSource();
        source.setName(name.trim());
        source.setActive(true);
        return leadSourceRepository.save(source);
    }

    public LeadSource updateSource(Long id, String name, Boolean active) {
        LeadSource source = leadSourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lead source not found with id: " + id));
        if (name != null && !name.isBlank()) {
            leadSourceRepository.findByNameIgnoreCase(name.trim()).ifPresent(existing -> {
                if (!existing.getId().equals(id)) {
                    throw new BadRequestException("A lead source with this name already exists");
                }
            });
            source.setName(name.trim());
        }
        if (active != null) {
            source.setActive(active);
        }
        return leadSourceRepository.save(source);
    }

    public void deleteSource(Long id) {
        LeadSource source = leadSourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lead source not found with id: " + id));
        leadSourceRepository.delete(source);
    }

    private LeadSource resolveSource(Long sourceId, String sourceName) {
        if (sourceId != null) {
            return leadSourceRepository.findById(sourceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Lead source not found with id: " + sourceId));
        }
        if (sourceName == null || sourceName.isBlank()) {
            throw new BadRequestException("Lead source is required");
        }
        return leadSourceRepository.findByNameIgnoreCase(sourceName.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Lead source not found: " + sourceName));
    }
}
