package com.futuremedia.services;

import com.futuremedia.models.BadRequestException;
import com.futuremedia.models.FreelancerApplication;
import com.futuremedia.models.FreelancerRole;
import com.futuremedia.models.ResourceNotFoundException;
import com.futuremedia.repositories.FreelancerApplicationRepository;
import com.futuremedia.repositories.FreelancerRoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class FreelancerApplicationService {

    @Autowired
    private FreelancerApplicationRepository freelancerApplicationRepository;

    @Autowired
    private FreelancerRoleRepository freelancerRoleRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private LeadService leadService;

    public FreelancerApplication apply(Long roleId, String name, String surname, String email, String telephone,
                                       String location, String skill, String experience, String namibian,
                                       MultipartFile cv) {
        if (roleId == null) {
            throw new BadRequestException("Role is required");
        }
        require(name, "Name is required");
        require(surname, "Surname is required");
        require(email, "Email is required");
        require(telephone, "Telephone is required");
        require(skill, "Skill is required");
        require(experience, "Experience is required");
        require(namibian, "Please indicate whether you are Namibian or not");
        if (cv == null || cv.isEmpty()) {
            throw new BadRequestException("CV document is required");
        }

        FreelancerRole role = freelancerRoleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Freelancer role not found with id: " + roleId));
        if (!role.isPublished()) {
            throw new BadRequestException("This role is not currently open");
        }

        FreelancerApplication application = new FreelancerApplication();
        application.setRole(role);
        application.setName(name.trim());
        application.setSurname(surname.trim());
        application.setEmail(email.trim().toLowerCase());
        application.setTelephone(telephone.trim());
        application.setLocation(location == null ? null : location.trim());
        application.setSkill(skill.trim());
        application.setExperience(experience.trim());
        application.setNamibian(parseYesNo(namibian));
        application.setCvPath(fileStorageService.storeDocumentOnly(cv, "freelancers"));
        FreelancerApplication saved = freelancerApplicationRepository.save(application);
        leadService.captureQuietly(saved.getName() + " " + saved.getSurname(), saved.getEmail(),
                saved.getTelephone(), saved.getLocation(), "Freelancer");
        return saved;
    }

    public List<FreelancerApplication> getAll() {
        return freelancerApplicationRepository.findAllByOrderByAppliedDateDesc();
    }

    public List<FreelancerApplication> getByRole(Long roleId) {
        FreelancerRole role = freelancerRoleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Freelancer role not found with id: " + roleId));
        return freelancerApplicationRepository.findByRoleOrderByAppliedDateDesc(role);
    }

    public List<FreelancerApplication> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return getAll();
        }
        return freelancerApplicationRepository.searchApplicants(keyword.trim());
    }

    public FreelancerApplication getById(Long id) {
        return freelancerApplicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Freelancer application not found with id: " + id));
    }

    public ResponseEntity<InputStreamResource> downloadCv(Long id) {
        FreelancerApplication application = getById(id);
        String fileName = fileNameFromPath(application.getCvPath(), "cv");
        String contentType = fileStorageService.detectStoredContentType(application.getCvPath());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(new InputStreamResource(fileStorageService.openStream(application.getCvPath())));
    }

    public void delete(Long id) {
        FreelancerApplication application = getById(id);
        fileStorageService.deleteFile(application.getCvPath());
        freelancerApplicationRepository.delete(application);
    }

    private void require(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(message);
        }
    }

    private boolean parseYesNo(String value) {
        String normalized = value.trim().toLowerCase();
        if (normalized.equals("true") || normalized.equals("yes") || normalized.equals("y") || normalized.equals("1")) {
            return true;
        }
        if (normalized.equals("false") || normalized.equals("no") || normalized.equals("n") || normalized.equals("0")) {
            return false;
        }
        throw new BadRequestException("Namibian must be Yes or No");
    }

    private String fileNameFromPath(String path, String fallback) {
        if (path == null || path.isBlank()) {
            return fallback;
        }
        String key = fileStorageService.extractObjectKey(path);
        if (key == null || !key.contains("/")) {
            return fallback;
        }
        return key.substring(key.lastIndexOf('/') + 1);
    }
}
