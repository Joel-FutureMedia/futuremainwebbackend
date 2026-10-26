package com.futuremedia.controllers;

import com.futuremedia.models.ApiResponse;
import com.futuremedia.models.Gallery;
import com.futuremedia.models.User;
import com.futuremedia.services.AuthService;
import com.futuremedia.services.GalleryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/gallery")
public class GalleryController {

    @Autowired
    private GalleryService galleryService;

    @Autowired
    private AuthService authService;

    @PostMapping(value = "/single", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Gallery>> uploadSingle(
            @RequestParam(required = false) String title,
            @RequestParam("image") MultipartFile image,
            Authentication authentication) {

        User user = authService.getCurrentUser(authentication.getName());
        Gallery gallery = galleryService.uploadSingle(title, image, user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Image uploaded successfully", gallery));
    }

    @PostMapping(value = "/multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<List<Gallery>>> uploadMultiple(
            @RequestParam(required = false) String title,
            @RequestParam("images") MultipartFile[] images,
            Authentication authentication) {

        User user = authService.getCurrentUser(authentication.getName());
        List<Gallery> galleries = galleryService.uploadMultiple(title, images, user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Images uploaded successfully", galleries));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Gallery>> updateImage(
            @PathVariable Long id,
            @RequestParam(required = false) String title,
            @RequestParam(value = "image", required = false) MultipartFile image) {

        Gallery gallery = galleryService.updateImage(id, title, image);
        return ResponseEntity.ok(ApiResponse.ok("Gallery image updated successfully", gallery));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteImage(@PathVariable Long id) {
        galleryService.deleteImage(id);
        return ResponseEntity.ok(ApiResponse.ok("Gallery image deleted successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Gallery>>> getAllAdmin() {
        return ResponseEntity.ok(ApiResponse.ok("Gallery retrieved successfully", galleryService.getAll()));
    }

    @GetMapping("/public")
    public ResponseEntity<ApiResponse<List<Gallery>>> getPublic() {
        return ResponseEntity.ok(ApiResponse.ok("Gallery retrieved successfully", galleryService.getAll()));
    }

    @GetMapping("/public/{id}")
    public ResponseEntity<ApiResponse<Gallery>> getPublicById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Gallery image retrieved", galleryService.getById(id)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Gallery>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Gallery image retrieved", galleryService.getById(id)));
    }
}
