package com.futuremedia.controllers;

import com.futuremedia.models.ApiResponse;
import com.futuremedia.models.Lead;
import com.futuremedia.models.LeadSource;
import com.futuremedia.services.LeadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/leads")
public class LeadController {

    @Autowired
    private LeadService leadService;

    @GetMapping("/sources/public")
    public ResponseEntity<ApiResponse<List<LeadSource>>> getSources() {
        return ResponseEntity.ok(ApiResponse.ok("Lead sources retrieved", leadService.getActiveSources()));
    }

    @GetMapping("/sources")
    public ResponseEntity<ApiResponse<List<LeadSource>>> getAllSources() {
        return ResponseEntity.ok(ApiResponse.ok("Lead sources retrieved", leadService.getAllSources()));
    }

    @GetMapping("/sources/{id}")
    public ResponseEntity<ApiResponse<LeadSource>> getSource(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Lead source retrieved", leadService.getSourceById(id)));
    }

    @PostMapping("/sources")
    public ResponseEntity<ApiResponse<LeadSource>> createSource(@RequestBody Map<String, Object> body) {
        LeadSource source = leadService.createSource(asString(body.get("name")));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Lead source created", source));
    }

    @PutMapping("/sources/{id}")
    public ResponseEntity<ApiResponse<LeadSource>> updateSource(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Boolean active = body.get("active") == null ? null : Boolean.valueOf(body.get("active").toString());
        LeadSource source = leadService.updateSource(id, asString(body.get("name")), active);
        return ResponseEntity.ok(ApiResponse.ok("Lead source updated", source));
    }

    @DeleteMapping("/sources/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteSource(@PathVariable Long id) {
        leadService.deleteSource(id);
        return ResponseEntity.ok(ApiResponse.ok("Lead source deleted"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Lead>> capture(@RequestBody Map<String, Object> body) {
        Lead lead = leadService.capture(
                asString(body.get("name")),
                asString(body.get("email")),
                asString(body.get("telephone")),
                asString(body.get("company")),
                asLong(body.get("sourceId")),
                asString(body.get("sourceName"))
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Lead captured successfully", lead));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Lead>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok("Leads retrieved", leadService.getAll()));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<Lead>>> search(@RequestParam String keyword) {
        return ResponseEntity.ok(ApiResponse.ok("Search results", leadService.search(keyword)));
    }

    @GetMapping("/source/{sourceId}")
    public ResponseEntity<ApiResponse<List<Lead>>> getBySource(@PathVariable Long sourceId) {
        return ResponseEntity.ok(ApiResponse.ok("Leads retrieved", leadService.getBySource(sourceId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Lead>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Lead retrieved", leadService.getById(id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> delete(@PathVariable Long id) {
        leadService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Lead deleted"));
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }

    private Long asLong(Object value) {
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        return Long.valueOf(value.toString());
    }
}
