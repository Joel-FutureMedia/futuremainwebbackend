package com.futuremedia.controllers;

import com.futuremedia.models.ApiResponse;
import com.futuremedia.models.Banner;
import com.futuremedia.models.User;
import com.futuremedia.services.AuthService;
import com.futuremedia.services.BannerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/banners")
public class BannerController {

    @Autowired
    private BannerService bannerService;

    @Autowired
    private AuthService authService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Banner>> createBanner(
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam("image") MultipartFile image,
            Authentication authentication) {

        User user = authService.getCurrentUser(authentication.getName());
        Banner banner = bannerService.createBanner(title, description, image, user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Banner created successfully", banner));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Banner>> updateBanner(
            @PathVariable Long id,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Boolean active,
            @RequestParam(value = "image", required = false) MultipartFile image) {

        Banner banner = bannerService.updateBanner(id, title, description, image, active);
        return ResponseEntity.ok(ApiResponse.ok("Banner updated successfully", banner));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteBanner(@PathVariable Long id) {
        bannerService.deleteBanner(id);
        return ResponseEntity.ok(ApiResponse.ok("Banner deleted successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Banner>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok("Banners retrieved", bannerService.getAll()));
    }

    @GetMapping("/public/latest")
    public ResponseEntity<ApiResponse<Banner>> getLatestActive() {
        return ResponseEntity.ok(ApiResponse.ok("Latest active banner retrieved", bannerService.getLatestActive()));
    }

    @GetMapping("/public")
    public ResponseEntity<ApiResponse<List<Banner>>> getPublicBanners() {
        return ResponseEntity.ok(ApiResponse.ok("Banners retrieved", bannerService.getAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Banner>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Banner retrieved", bannerService.getById(id)));
    }
}
