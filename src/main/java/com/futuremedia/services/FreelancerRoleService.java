package com.futuremedia.services;

import com.futuremedia.models.BadRequestException;
import com.futuremedia.models.FreelancerRole;
import com.futuremedia.models.ResourceNotFoundException;
import com.futuremedia.models.User;
import com.futuremedia.repositories.FreelancerApplicationRepository;
import com.futuremedia.repositories.FreelancerRoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class FreelancerRoleService {

    @Autowired
    private FreelancerRoleRepository freelancerRoleRepository;

    @Autowired
    private FreelancerApplicationRepository freelancerApplicationRepository;

    @Autowired
    private FileStorageService fileStorageService;

    public FreelancerRole create(Map<String, Object> body, User user) {
        FreelancerRole role = new FreelancerRole();
        role.setCreatedBy(user);
        apply(role, body, true);
        return freelancerRoleRepository.save(role);
    }

    public FreelancerRole update(Long id, Map<String, Object> body) {
        FreelancerRole role = getById(id);
        apply(role, body, false);
        return freelancerRoleRepository.save(role);
    }

    public void delete(Long id) {
        FreelancerRole role = getById(id);
        freelancerApplicationRepository.findByRoleOrderByAppliedDateDesc(role).forEach(application -> {
            fileStorageService.deleteFile(application.getCvPath());
            freelancerApplicationRepository.delete(application);
        });
        freelancerRoleRepository.delete(role);
    }

    public FreelancerRole publish(Long id) {
        FreelancerRole role = getById(id);
        role.setPublished(true);
        return freelancerRoleRepository.save(role);
    }

    public FreelancerRole unpublish(Long id) {
        FreelancerRole role = getById(id);
        role.setPublished(false);
        return freelancerRoleRepository.save(role);
    }

    public List<FreelancerRole> getAll() {
        return freelancerRoleRepository.findAllByOrderByCreatedDateDesc();
    }

    public List<FreelancerRole> getPublished() {
        return freelancerRoleRepository.findByPublishedTrueOrderByCreatedDateDesc();
    }

    public FreelancerRole getById(Long id) {
        return freelancerRoleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Freelancer role not found with id: " + id));
    }

    private void apply(FreelancerRole role, Map<String, Object> body, boolean creating) {
        if (body == null) {
            throw new BadRequestException("Role details are required");
        }
        if (body.containsKey("title") || creating) {
            String title = text(body.get("title"));
            if (title == null) {
                throw new BadRequestException("Title is required");
            }
            role.setTitle(title);
        }
        if (body.containsKey("description") || creating) {
            String description = text(body.get("description"));
            if (description == null) {
                throw new BadRequestException("Description is required");
            }
            role.setDescription(description);
        }
        if (body.containsKey("closingDate") || creating) {
            role.setClosingDate(date(body.get("closingDate")));
        }
        if (body.containsKey("published") || creating) {
            role.setPublished(bool(body.get("published"), true));
        }
    }

    private String text(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private LocalDate date(Object value) {
        String text = text(value);
        if (text == null) {
            return null;
        }
        try {
            return LocalDate.parse(text);
        } catch (Exception e) {
            throw new BadRequestException("Closing date must be in YYYY-MM-DD format");
        }
    }

    private boolean bool(Object value, boolean fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Boolean flag) {
            return flag;
        }
        String text = String.valueOf(value).trim().toLowerCase();
        return text.equals("true") || text.equals("yes") || text.equals("1");
    }
}
