package com.futuremedia.services;

import com.futuremedia.models.*;
import com.futuremedia.repositories.JobApplicationRepository;
import com.futuremedia.repositories.VacancyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.regex.Pattern;

@Service
public class JobApplicationService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    @Autowired
    private JobApplicationRepository jobApplicationRepository;

    @Autowired
    private VacancyRepository vacancyRepository;

    @Autowired
    private FileStorageService fileStorageService;

    public JobApplication apply(Long vacancyId, String fullName, String email, String phone,
                                String location, MultipartFile cv) {
        if (vacancyId == null) {
            throw new BadRequestException("Vacancy ID is required");
        }
        if (fullName == null || fullName.isBlank()) {
            throw new BadRequestException("Full name is required");
        }
        if (email == null || email.isBlank() || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new BadRequestException("Valid email is required");
        }
        if (location == null || location.isBlank()) {
            throw new BadRequestException("Location is required");
        }
        if (cv == null || cv.isEmpty()) {
            throw new BadRequestException("CV document is required");
        }

        Vacancy vacancy = vacancyRepository.findById(vacancyId)
                .orElseThrow(() -> new ResourceNotFoundException("Vacancy not found with id: " + vacancyId));

        if (!vacancy.isPublished()) {
            throw new BadRequestException("This vacancy is not currently open for applications");
        }

        JobApplication application = new JobApplication();
        application.setVacancy(vacancy);
        application.setFullName(fullName.trim());
        application.setEmail(email.trim().toLowerCase());
        application.setPhone(phone != null ? phone.trim() : null);
        application.setLocation(location.trim());
        application.setCvPath(fileStorageService.storeDocumentOnly(cv, "applications"));
        return jobApplicationRepository.save(application);
    }

    public List<JobApplication> getAllApplications() {
        return jobApplicationRepository.findAllByOrderByAppliedDateDesc();
    }

    public List<JobApplication> getByVacancy(Long vacancyId) {
        Vacancy vacancy = vacancyRepository.findById(vacancyId)
                .orElseThrow(() -> new ResourceNotFoundException("Vacancy not found with id: " + vacancyId));
        return jobApplicationRepository.findByVacancyOrderByAppliedDateDesc(vacancy);
    }

    public List<JobApplication> searchApplicants(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return getAllApplications();
        }
        return jobApplicationRepository.searchApplicants(keyword.trim());
    }

    public JobApplication getById(Long id) {
        return jobApplicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + id));
    }

    public void deleteApplication(Long id) {
        JobApplication application = getById(id);
        fileStorageService.deleteFile(application.getCvPath());
        jobApplicationRepository.delete(application);
    }
}
