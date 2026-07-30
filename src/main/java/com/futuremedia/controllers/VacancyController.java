package com.futuremedia.controllers;

import com.futuremedia.models.ApiResponse;
import com.futuremedia.models.User;
import com.futuremedia.models.Vacancy;
import com.futuremedia.services.AuthService;
import com.futuremedia.services.VacancyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/vacancies")
public class VacancyController {

    @Autowired
    private VacancyService vacancyService;

    @Autowired
    private AuthService authService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Vacancy>> createVacancy(
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam(required = false) String closingDate,
            @RequestParam(defaultValue = "true") boolean published,
            @RequestParam("media") MultipartFile media,
            Authentication authentication) {

        User user = authService.getCurrentUser(authentication.getName());
        Vacancy vacancy = vacancyService.createVacancy(title, description, closingDate, media, published, user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Vacancy created successfully", vacancy));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Vacancy>> updateVacancy(
            @PathVariable Long id,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String closingDate,
            @RequestParam(required = false) Boolean published,
            @RequestParam(value = "media", required = false) MultipartFile media) {

        Vacancy vacancy = vacancyService.updateVacancy(id, title, description, closingDate, media, published);
        return ResponseEntity.ok(ApiResponse.ok("Vacancy updated successfully", vacancy));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteVacancy(@PathVariable Long id) {
        vacancyService.deleteVacancy(id);
        return ResponseEntity.ok(ApiResponse.ok("Vacancy deleted successfully"));
    }

    @PutMapping("/{id}/publish")
    public ResponseEntity<ApiResponse<Vacancy>> publish(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Vacancy published", vacancyService.publishVacancy(id)));
    }

    @PutMapping("/{id}/unpublish")
    public ResponseEntity<ApiResponse<Vacancy>> unpublish(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Vacancy unpublished", vacancyService.unpublishVacancy(id)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Vacancy>>> getAllAdmin() {
        return ResponseEntity.ok(ApiResponse.ok("Vacancies retrieved", vacancyService.getAllVacancies()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Vacancy>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Vacancy retrieved", vacancyService.getById(id)));
    }

    @GetMapping("/public")
    public ResponseEntity<ApiResponse<List<Vacancy>>> getPublic() {
        return ResponseEntity.ok(ApiResponse.ok("Published vacancies retrieved", vacancyService.getPublishedVacancies()));
    }

    @GetMapping("/public/{id}")
    public ResponseEntity<ApiResponse<Vacancy>> getPublicById(@PathVariable Long id) {
        Vacancy vacancy = vacancyService.getById(id);
        if (!vacancy.isPublished()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.fail("Vacancy not found or not published"));
        }
        return ResponseEntity.ok(ApiResponse.ok("Vacancy retrieved", vacancy));
    }
}
