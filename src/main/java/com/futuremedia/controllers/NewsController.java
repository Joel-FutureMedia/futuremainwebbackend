package com.futuremedia.controllers;

import com.futuremedia.models.ApiResponse;
import com.futuremedia.models.News;
import com.futuremedia.models.User;
import com.futuremedia.services.AuthService;
import com.futuremedia.services.NewsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/news")
public class NewsController {

    @Autowired
    private NewsService newsService;

    @Autowired
    private AuthService authService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<News>> createNews(
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam("media") MultipartFile media,
            Authentication authentication) {

        User user = authService.getCurrentUser(authentication.getName());
        News news = newsService.createNews(title, description, media, user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("News created successfully", news));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<News>> updateNews(
            @PathVariable Long id,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description,
            @RequestParam(value = "media", required = false) MultipartFile media,
            Authentication authentication) {

        User user = authService.getCurrentUser(authentication.getName());
        News news = newsService.updateNews(id, title, description, media, user);
        return ResponseEntity.ok(ApiResponse.ok("News updated successfully", news));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteNews(
            @PathVariable Long id,
            Authentication authentication) {

        User user = authService.getCurrentUser(authentication.getName());
        newsService.deleteNews(id, user);
        return ResponseEntity.ok(ApiResponse.ok("News deleted successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<News>>> getAllNews() {
        return ResponseEntity.ok(ApiResponse.ok("News retrieved successfully", newsService.getAllNews()));
    }

    @GetMapping("/public")
    public ResponseEntity<ApiResponse<List<News>>> getApprovedNews() {
        return ResponseEntity.ok(ApiResponse.ok("Approved news retrieved", newsService.getApprovedNews()));
    }

    @GetMapping("/public/{id}")
    public ResponseEntity<ApiResponse<News>> getApprovedNewsById(@PathVariable Long id) {
        News news = newsService.getById(id);
        if (news.getApprovalStatus() != com.futuremedia.models.ApprovalStatus.APPROVED) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.fail("News not found or not approved"));
        }
        return ResponseEntity.ok(ApiResponse.ok("News retrieved successfully", news));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<News>>> getMyNews(Authentication authentication) {
        User user = authService.getCurrentUser(authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok("Your news retrieved successfully", newsService.getMyNews(user)));
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<News>>> getPendingNews() {
        return ResponseEntity.ok(ApiResponse.ok("Pending news retrieved", newsService.getPendingNews()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<News>> getNews(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("News retrieved successfully", newsService.getById(id)));
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<News>> approveNews(
            @PathVariable Long id,
            Authentication authentication) {

        User user = authService.getCurrentUser(authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok("News approved successfully", newsService.approveNews(id, user)));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<News>> rejectNews(
            @PathVariable Long id,
            Authentication authentication) {

        User user = authService.getCurrentUser(authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok("News rejected", newsService.rejectNews(id, user)));
    }
}
