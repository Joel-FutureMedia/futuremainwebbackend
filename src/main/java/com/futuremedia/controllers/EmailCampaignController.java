package com.futuremedia.controllers;

import com.futuremedia.models.ApiResponse;
import com.futuremedia.models.EmailAudienceType;
import com.futuremedia.models.EmailCampaign;
import com.futuremedia.models.EmailCampaignImage;
import com.futuremedia.models.EmailCampaignRecipient;
import com.futuremedia.models.User;
import com.futuremedia.services.AuthService;
import com.futuremedia.services.EmailCampaignService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/email-campaigns")
public class EmailCampaignController {

    @Autowired
    private EmailCampaignService emailCampaignService;

    @Autowired
    private AuthService authService;

    @GetMapping("/audiences")
    public ResponseEntity<ApiResponse<List<String>>> audiences() {
        List<String> values = Arrays.stream(EmailAudienceType.values()).map(Enum::name).toList();
        return ResponseEntity.ok(ApiResponse.ok("Audience types retrieved", values));
    }

    @GetMapping("/audience")
    public ResponseEntity<ApiResponse<Map<String, Object>>> previewAudience(
            @RequestParam(required = false, defaultValue = "ALL_CAPTURED") String audience,
            @RequestParam(required = false) Long leadSourceId) {
        return ResponseEntity.ok(ApiResponse.ok("Audience preview retrieved",
                emailCampaignService.previewAudience(audience, leadSourceId)));
    }

    @GetMapping("/preview/{id}")
    public ResponseEntity<ApiResponse<Map<String, String>>> previewByPath(
            @PathVariable Long id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String company) {
        return preview(id, name, email, company);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EmailCampaign>> create(
            @RequestBody Map<String, Object> body,
            Authentication authentication) {
        User user = authService.getCurrentUser(authentication.getName());
        EmailCampaign campaign = emailCampaignService.create(body, user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Campaign created as draft", campaign));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EmailCampaign>> update(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(ApiResponse.ok("Campaign updated", emailCampaignService.updateDraft(id, body)));
    }

    @PostMapping("/{id}/broadcast")
    public ResponseEntity<ApiResponse<EmailCampaign>> broadcast(@PathVariable Long id) {
        EmailCampaign campaign = emailCampaignService.broadcast(id);
        return ResponseEntity.ok(ApiResponse.ok(
                "Broadcast started. Emails are being sent in the background.", campaign));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<EmailCampaign>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok("Campaigns retrieved", emailCampaignService.getAll()));
    }

    @GetMapping("/{id}/recipients")
    public ResponseEntity<ApiResponse<List<EmailCampaignRecipient>>> getRecipients(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Campaign recipients retrieved",
                emailCampaignService.getRecipients(id)));
    }

    @GetMapping("/{id}/preview")
    public ResponseEntity<ApiResponse<Map<String, String>>> preview(
            @PathVariable Long id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String company) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("html", emailCampaignService.preview(id, name, email, company));
        return ResponseEntity.ok(ApiResponse.ok("Campaign preview generated", data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EmailCampaign>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Campaign retrieved", emailCampaignService.getById(id)));
    }

    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<EmailCampaignImage>> addImage(
            @PathVariable Long id,
            @RequestParam(required = false) String caption,
            @RequestParam("file") MultipartFile file) {
        EmailCampaignImage image = emailCampaignService.addImage(id, caption, file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Campaign image uploaded", image));
    }

    @DeleteMapping("/{id}/images/{imageId}")
    public ResponseEntity<ApiResponse<Object>> deleteImage(@PathVariable Long id, @PathVariable Long imageId) {
        emailCampaignService.deleteImage(id, imageId);
        return ResponseEntity.ok(ApiResponse.ok("Campaign image deleted"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> delete(@PathVariable Long id) {
        emailCampaignService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Campaign deleted"));
    }
}
