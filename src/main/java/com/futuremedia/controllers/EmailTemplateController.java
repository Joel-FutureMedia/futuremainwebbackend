package com.futuremedia.controllers;

import com.futuremedia.models.ApiResponse;
import com.futuremedia.models.EmailTemplate;
import com.futuremedia.models.EmailTemplateImage;
import com.futuremedia.models.User;
import com.futuremedia.services.AuthService;
import com.futuremedia.services.EmailTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/email-templates")
public class EmailTemplateController {

    @Autowired
    private EmailTemplateService emailTemplateService;

    @Autowired
    private AuthService authService;

    @GetMapping("/defaults")
    public ResponseEntity<ApiResponse<Map<String, String>>> defaults() {
        return ResponseEntity.ok(ApiResponse.ok("Template defaults retrieved", emailTemplateService.defaults()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EmailTemplate>> create(
            @RequestBody Map<String, Object> body,
            Authentication authentication) {
        User user = authService.getCurrentUser(authentication.getName());
        EmailTemplate template = emailTemplateService.create(body, user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Email template created", template));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EmailTemplate>> update(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(ApiResponse.ok("Email template updated", emailTemplateService.update(id, body)));
    }

    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<EmailTemplateImage>> addImage(
            @PathVariable Long id,
            @RequestParam String section,
            @RequestParam(required = false) String caption,
            @RequestParam("file") MultipartFile file) {
        EmailTemplateImage image = emailTemplateService.addImage(id, section, caption, file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Template image uploaded", image));
    }

    @DeleteMapping("/{id}/images/{imageId}")
    public ResponseEntity<ApiResponse<Object>> deleteImage(@PathVariable Long id, @PathVariable Long imageId) {
        emailTemplateService.deleteImage(id, imageId);
        return ResponseEntity.ok(ApiResponse.ok("Template image deleted"));
    }

    @GetMapping("/{id}/preview")
    public ResponseEntity<ApiResponse<Map<String, String>>> preview(
            @PathVariable Long id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String company) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("html", emailTemplateService.preview(id, name, email, company));
        return ResponseEntity.ok(ApiResponse.ok("Template preview generated", data));
    }

    @PostMapping("/{id}/test")
    public ResponseEntity<ApiResponse<Object>> sendTest(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        emailTemplateService.sendTest(
                id,
                asString(body.get("email")),
                asString(body.get("name")),
                asString(body.get("company"))
        );
        return ResponseEntity.ok(ApiResponse.ok("Test email sent"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<EmailTemplate>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok("Email templates retrieved", emailTemplateService.getAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EmailTemplate>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Email template retrieved", emailTemplateService.getById(id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> delete(@PathVariable Long id) {
        emailTemplateService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Email template deleted"));
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }
}
