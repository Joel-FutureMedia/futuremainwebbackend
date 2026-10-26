package com.futuremedia.services;

import com.futuremedia.models.BadRequestException;
import com.futuremedia.models.JobApplication;
import com.futuremedia.models.ResourceNotFoundException;
import com.futuremedia.models.Vacancy;
import com.futuremedia.repositories.JobApplicationRepository;
import com.futuremedia.repositories.VacancyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@Service
public class JobApplicationService {

    @Autowired
    private JobApplicationRepository jobApplicationRepository;

    @Autowired
    private VacancyRepository vacancyRepository;

    @Autowired
    private FileStorageService fileStorageService;

    public JobApplication apply(Long vacancyId, String name, String surname, String dateOfBirth,
                                String highestQualification, String workExperience,
                                String namibian, MultipartFile cv) {
        if (vacancyId == null) {
            throw new BadRequestException("Vacancy ID is required");
        }
        if (name == null || name.isBlank()) {
            throw new BadRequestException("Name is required");
        }
        if (surname == null || surname.isBlank()) {
            throw new BadRequestException("Surname is required");
        }
        if (dateOfBirth == null || dateOfBirth.isBlank()) {
            throw new BadRequestException("Date of birth is required");
        }
        if (highestQualification == null || highestQualification.isBlank()) {
            throw new BadRequestException("Highest qualification is required");
        }
        if (workExperience == null || workExperience.isBlank()) {
            throw new BadRequestException("Work experience is required");
        }
        if (namibian == null || namibian.isBlank()) {
            throw new BadRequestException("Please indicate whether you are Namibian or not");
        }
        if (cv == null || cv.isEmpty()) {
            throw new BadRequestException("CV document is required");
        }

        LocalDate dob;
        try {
            dob = LocalDate.parse(dateOfBirth.trim());
        } catch (Exception e) {
            throw new BadRequestException("Date of birth must be in YYYY-MM-DD format");
        }

        Vacancy vacancy = vacancyRepository.findById(vacancyId)
                .orElseThrow(() -> new ResourceNotFoundException("Vacancy not found with id: " + vacancyId));

        if (!vacancy.isPublished()) {
            throw new BadRequestException("This vacancy is not currently open for applications");
        }

        JobApplication application = new JobApplication();
        application.setVacancy(vacancy);
        application.setName(name.trim());
        application.setSurname(surname.trim());
        application.setDateOfBirth(dob);
        application.setHighestQualification(highestQualification.trim());
        application.setWorkExperience(workExperience.trim());
        application.setNamibian(parseYesNo(namibian));
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

    public ResponseEntity<InputStreamResource> downloadCv(Long id) {
        JobApplication application = getById(id);
        String fileName = fileNameFromPath(application.getCvPath(), "cv");
        String contentType = fileStorageService.detectStoredContentType(application.getCvPath());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(new InputStreamResource(fileStorageService.openStream(application.getCvPath())));
    }

    public void deleteApplication(Long id) {
        JobApplication application = getById(id);
        fileStorageService.deleteFile(application.getCvPath());
        jobApplicationRepository.delete(application);
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
