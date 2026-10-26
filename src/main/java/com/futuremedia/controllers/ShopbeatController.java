package com.futuremedia.controllers;

import com.futuremedia.models.ApiResponse;
import com.futuremedia.models.ShopbeatDemoRequest;
import com.futuremedia.models.ShopbeatFeeling;
import com.futuremedia.services.ShopbeatDemoRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/shopbeat")
public class ShopbeatController {

    @Autowired
    private ShopbeatDemoRequestService shopbeatDemoRequestService;

    @GetMapping("/feelings/public")
    public ResponseEntity<ApiResponse<List<ShopbeatFeeling>>> getFeelings() {
        return ResponseEntity.ok(ApiResponse.ok("Feelings retrieved", shopbeatDemoRequestService.getActiveFeelings()));
    }

    @GetMapping("/feelings")
    public ResponseEntity<ApiResponse<List<ShopbeatFeeling>>> getAllFeelings() {
        return ResponseEntity.ok(ApiResponse.ok("Feelings retrieved", shopbeatDemoRequestService.getAllFeelings()));
    }

    @GetMapping("/feelings/{id}")
    public ResponseEntity<ApiResponse<ShopbeatFeeling>> getFeeling(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Feeling retrieved", shopbeatDemoRequestService.getFeelingById(id)));
    }

    @PostMapping("/feelings")
    public ResponseEntity<ApiResponse<ShopbeatFeeling>> createFeeling(@RequestBody Map<String, Object> body) {
        ShopbeatFeeling feeling = shopbeatDemoRequestService.createFeeling(asString(body.get("name")));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Feeling created", feeling));
    }

    @PutMapping("/feelings/{id}")
    public ResponseEntity<ApiResponse<ShopbeatFeeling>> updateFeeling(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Boolean active = body.get("active") == null ? null : Boolean.valueOf(body.get("active").toString());
        ShopbeatFeeling feeling = shopbeatDemoRequestService.updateFeeling(id, asString(body.get("name")), active);
        return ResponseEntity.ok(ApiResponse.ok("Feeling updated", feeling));
    }

    @DeleteMapping("/feelings/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteFeeling(@PathVariable Long id) {
        shopbeatDemoRequestService.deleteFeeling(id);
        return ResponseEntity.ok(ApiResponse.ok("Feeling deleted"));
    }

    @PostMapping("/demo")
    public ResponseEntity<ApiResponse<ShopbeatDemoRequest>> submit(@RequestBody Map<String, Object> body) {
        ShopbeatDemoRequest request = shopbeatDemoRequestService.submit(
                asString(body.get("name")),
                asString(body.get("email")),
                asString(body.get("telephone")),
                asString(body.get("company")),
                asLong(body.get("feelingId")),
                asBoolean(body.get("hasSoundInspiration")),
                asString(body.get("soundInspiration")),
                asString(body.get("message"))
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Shopbeat demo request submitted successfully", request));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ShopbeatDemoRequest>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok("Shopbeat demo requests retrieved", shopbeatDemoRequestService.getAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ShopbeatDemoRequest>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Shopbeat demo request retrieved", shopbeatDemoRequestService.getById(id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> delete(@PathVariable Long id) {
        shopbeatDemoRequestService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Shopbeat demo request deleted"));
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

    private Boolean asBoolean(Object value) {
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        String normalized = value.toString().trim().toLowerCase();
        if (normalized.equals("true") || normalized.equals("yes") || normalized.equals("y") || normalized.equals("1")) {
            return true;
        }
        if (normalized.equals("false") || normalized.equals("no") || normalized.equals("n") || normalized.equals("0")) {
            return false;
        }
        return Boolean.valueOf(normalized);
    }
}
