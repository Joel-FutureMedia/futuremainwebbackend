package com.futuremedia.controllers;

import com.futuremedia.models.ApiResponse;
import com.futuremedia.models.JobApplication;
import com.futuremedia.services.JobApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
public class JobApplicationController {

    @Autowired
    private JobApplicationService jobApplicationService;

    @PostMapping(value = "/apply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<JobApplication>> apply(
            @RequestParam Long vacancyId,
            @RequestParam String fullName,
            @RequestParam String email,
            @RequestParam(required = false) String phone,
            @RequestParam String location,
            @RequestParam("cv") MultipartFile cv) {

        JobApplication application = jobApplicationService.apply(
                vacancyId, fullName, email, phone, location, cv);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Application submitted successfully", application));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<JobApplication>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok("Applications retrieved", jobApplicationService.getAllApplications()));
    }

    @GetMapping("/vacancy/{vacancyId}")
    public ResponseEntity<ApiResponse<List<JobApplication>>> getByVacancy(@PathVariable Long vacancyId) {
        return ResponseEntity.ok(ApiResponse.ok("Applications retrieved",
                jobApplicationService.getByVacancy(vacancyId)));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<JobApplication>>> search(@RequestParam String keyword) {
        return ResponseEntity.ok(ApiResponse.ok("Search results",
                jobApplicationService.searchApplicants(keyword)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<JobApplication>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Application retrieved", jobApplicationService.getById(id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> delete(@PathVariable Long id) {
        jobApplicationService.deleteApplication(id);
        return ResponseEntity.ok(ApiResponse.ok("Application deleted successfully"));
    }
}
