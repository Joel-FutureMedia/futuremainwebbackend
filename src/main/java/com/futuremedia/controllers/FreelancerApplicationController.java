package com.futuremedia.controllers;

import com.futuremedia.models.ApiResponse;
import com.futuremedia.models.FreelancerApplication;
import com.futuremedia.services.FreelancerApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/freelancer-applications")
public class FreelancerApplicationController {

    @Autowired
    private FreelancerApplicationService freelancerApplicationService;

    @PostMapping(value = "/apply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<FreelancerApplication>> apply(
            @RequestParam Long roleId,
            @RequestParam String name,
            @RequestParam String surname,
            @RequestParam String email,
            @RequestParam String telephone,
            @RequestParam(required = false) String location,
            @RequestParam String skill,
            @RequestParam String experience,
            @RequestParam String namibian,
            @RequestParam("cv") MultipartFile cv) {

        FreelancerApplication application = freelancerApplicationService.apply(
                roleId, name, surname, email, telephone, location, skill, experience, namibian, cv);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Application submitted successfully", application));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FreelancerApplication>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok("Freelancer applications retrieved", freelancerApplicationService.getAll()));
    }

    @GetMapping("/role/{roleId}")
    public ResponseEntity<ApiResponse<List<FreelancerApplication>>> getByRole(@PathVariable Long roleId) {
        return ResponseEntity.ok(ApiResponse.ok("Freelancer applications retrieved", freelancerApplicationService.getByRole(roleId)));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<FreelancerApplication>>> search(@RequestParam String keyword) {
        return ResponseEntity.ok(ApiResponse.ok("Search results", freelancerApplicationService.search(keyword)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FreelancerApplication>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Freelancer application retrieved", freelancerApplicationService.getById(id)));
    }

    @GetMapping("/{id}/cv")
    public ResponseEntity<InputStreamResource> downloadCv(@PathVariable Long id) {
        return freelancerApplicationService.downloadCv(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> delete(@PathVariable Long id) {
        freelancerApplicationService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Freelancer application deleted"));
    }
}
