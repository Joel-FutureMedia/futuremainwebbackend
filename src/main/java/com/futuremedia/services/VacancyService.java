package com.futuremedia.services;

import com.futuremedia.models.*;
import com.futuremedia.repositories.JobApplicationRepository;
import com.futuremedia.repositories.VacancyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@Service
public class VacancyService {

    @Autowired
    private VacancyRepository vacancyRepository;

    @Autowired
    private JobApplicationRepository jobApplicationRepository;

    @Autowired
    private FileStorageService fileStorageService;

    public Vacancy createVacancy(String title, String description, String closingDate,
                                 MultipartFile media, boolean published, User currentUser) {
        if (title == null || title.isBlank()) {
            throw new BadRequestException("Title is required");
        }
        if (description == null || description.isBlank()) {
            throw new BadRequestException("Description is required");
        }
        if (media == null || media.isEmpty()) {
            throw new BadRequestException("Image or PDF is required");
        }

        Vacancy vacancy = new Vacancy();
        vacancy.setTitle(title.trim());
        vacancy.setDescription(description.trim());
        if (closingDate != null && !closingDate.isBlank()) {
            vacancy.setClosingDate(LocalDate.parse(closingDate));
        }
        vacancy.setMediaPath(fileStorageService.storeImageOrPdf(media, "vacancies"));
        vacancy.setMediaType(fileStorageService.detectMediaType(media));
        vacancy.setPublished(published);
        vacancy.setCreatedBy(currentUser);
        return vacancyRepository.save(vacancy);
    }

    public Vacancy updateVacancy(Long id, String title, String description, String closingDate,
                                 MultipartFile media, Boolean published) {
        Vacancy vacancy = getById(id);

        if (title != null && !title.isBlank()) {
            vacancy.setTitle(title.trim());
        }
        if (description != null && !description.isBlank()) {
            vacancy.setDescription(description.trim());
        }
        if (closingDate != null && !closingDate.isBlank()) {
            vacancy.setClosingDate(LocalDate.parse(closingDate));
        }
        if (media != null && !media.isEmpty()) {
            fileStorageService.deleteFile(vacancy.getMediaPath());
            vacancy.setMediaPath(fileStorageService.storeImageOrPdf(media, "vacancies"));
            vacancy.setMediaType(fileStorageService.detectMediaType(media));
        }
        if (published != null) {
            vacancy.setPublished(published);
        }

        return vacancyRepository.save(vacancy);
    }

    @Transactional
    public void deleteVacancy(Long id) {
        Vacancy vacancy = getById(id);
        // Remove dependent applications first to avoid FK abort (PostgreSQL 25P02)
        List<JobApplication> applications = jobApplicationRepository.findByVacancyOrderByAppliedDateDesc(vacancy);
        for (JobApplication application : applications) {
            fileStorageService.deleteFile(application.getCvPath());
            jobApplicationRepository.delete(application);
        }
        fileStorageService.deleteFile(vacancy.getMediaPath());
        vacancyRepository.delete(vacancy);
    }

    public Vacancy publishVacancy(Long id) {
        Vacancy vacancy = getById(id);
        vacancy.setPublished(true);
        return vacancyRepository.save(vacancy);
    }

    public Vacancy unpublishVacancy(Long id) {
        Vacancy vacancy = getById(id);
        vacancy.setPublished(false);
        return vacancyRepository.save(vacancy);
    }

    public Vacancy getById(Long id) {
        return vacancyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vacancy not found with id: " + id));
    }

    public List<Vacancy> getAllVacancies() {
        return vacancyRepository.findAllByOrderByCreatedDateDesc();
    }

    public List<Vacancy> getPublishedVacancies() {
        return vacancyRepository.findByPublishedTrueOrderByCreatedDateDesc();
    }
}
