package com.futuremedia.controllers;

import com.futuremedia.models.ApiResponse;
import com.futuremedia.models.GatedDocument;
import com.futuremedia.models.GatedDocumentRequest;
import com.futuremedia.models.User;
import com.futuremedia.services.AuthService;
import com.futuremedia.services.GatedDocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/gated-documents")
public class GatedDocumentController {

    @Autowired
    private GatedDocumentService gatedDocumentService;

    @Autowired
    private AuthService authService;

    @GetMapping("/public")
    public ResponseEntity<ApiResponse<List<GatedDocument>>> getPublic() {
        return ResponseEntity.ok(ApiResponse.ok("Documents retrieved", gatedDocumentService.getPublicActive()));
    }

    @GetMapping("/public/{id}")
    public ResponseEntity<ApiResponse<GatedDocument>> getPublicById(@PathVariable Long id) {
        GatedDocument document = gatedDocumentService.getById(id);
        if (!document.isActive()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.fail("Document not found or not available"));
        }
        return ResponseEntity.ok(ApiResponse.ok("Document retrieved", document));
    }

    @GetMapping("/download/{token}")
    public ResponseEntity<InputStreamResource> downloadByToken(@PathVariable String token) {
        return gatedDocumentService.downloadByToken(token);
    }

    @GetMapping("/requests")
    public ResponseEntity<ApiResponse<List<GatedDocumentRequest>>> getRequests() {
        return ResponseEntity.ok(ApiResponse.ok("Download requests retrieved", gatedDocumentService.getAllRequests()));
    }

    @PostMapping("/{id}/request")
    public ResponseEntity<ApiResponse<GatedDocumentRequest>> requestDownload(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        GatedDocumentRequest request = gatedDocumentService.requestDownload(
                id,
                asString(body.get("name")),
                asString(body.get("email")),
                asString(body.get("telephone")),
                asString(body.get("company"))
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Thank you. The document has been sent to your email.", request));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<GatedDocument>> create(
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        User user = authService.getCurrentUser(authentication.getName());
        GatedDocument document = gatedDocumentService.create(title, description, file, user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Gated document uploaded. Clients will see the title: " + document.getTitle(), document));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<GatedDocument>> update(
            @PathVariable Long id,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Boolean active,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        GatedDocument document = gatedDocumentService.update(id, title, description, file, active);
        return ResponseEntity.ok(ApiResponse.ok("Gated document updated", document));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<GatedDocument>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok("Gated documents retrieved", gatedDocumentService.getAll()));
    }

    @GetMapping("/{id}/requests")
    public ResponseEntity<ApiResponse<List<GatedDocumentRequest>>> getRequestsByDocument(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Download requests retrieved",
                gatedDocumentService.getRequestsByDocument(id)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GatedDocument>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Gated document retrieved", gatedDocumentService.getById(id)));
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<InputStreamResource> downloadById(@PathVariable Long id) {
        return gatedDocumentService.downloadById(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> delete(@PathVariable Long id) {
        gatedDocumentService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Gated document deleted"));
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }
}
