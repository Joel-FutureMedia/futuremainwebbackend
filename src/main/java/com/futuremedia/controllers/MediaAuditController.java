package com.futuremedia.controllers;

import com.futuremedia.models.ApiResponse;
import com.futuremedia.models.MediaAudit;
import com.futuremedia.services.MediaAuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/media-audit")
public class MediaAuditController {

    @Autowired
    private MediaAuditService mediaAuditService;

    @PostMapping
    public ResponseEntity<ApiResponse<MediaAudit>> submit(@RequestBody Map<String, Object> body) {
        MediaAudit audit = mediaAuditService.submit(body);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Media audit request submitted successfully", audit));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MediaAudit>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok("Media audits retrieved", mediaAuditService.getAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MediaAudit>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Media audit retrieved", mediaAuditService.getById(id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> delete(@PathVariable Long id) {
        mediaAuditService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Media audit deleted"));
    }
}
