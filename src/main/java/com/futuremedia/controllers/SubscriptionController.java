package com.futuremedia.controllers;

import com.futuremedia.models.ApiResponse;
import com.futuremedia.models.Subscription;
import com.futuremedia.services.SubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    @Autowired
    private SubscriptionService subscriptionService;

    @PostMapping
    public ResponseEntity<ApiResponse<Subscription>> subscribe(@RequestBody Map<String, String> body) {
        Subscription subscription = subscriptionService.subscribe(
                body.get("fullName"),
                body.get("email"),
                body.get("language")
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Subscribed successfully. Check your email for a welcome message.", subscription));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Subscription>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok("Subscriptions retrieved", subscriptionService.getAll()));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<Subscription>>> getActive() {
        return ResponseEntity.ok(ApiResponse.ok("Active subscriptions retrieved", subscriptionService.getActive()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Subscription>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Subscription retrieved", subscriptionService.getById(id)));
    }

    @PutMapping("/{id}/unsubscribe")
    public ResponseEntity<ApiResponse<Subscription>> unsubscribe(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Unsubscribed successfully", subscriptionService.unsubscribe(id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> delete(@PathVariable Long id) {
        subscriptionService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Subscription deleted"));
    }
}
