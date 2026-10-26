package com.futuremedia.controllers;

import com.futuremedia.models.ApiResponse;
import com.futuremedia.models.Event;
import com.futuremedia.models.User;
import com.futuremedia.services.AuthService;
import com.futuremedia.services.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

    @Autowired
    private EventService eventService;

    @Autowired
    private AuthService authService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Event>> createEvent(
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam("media") MultipartFile media,
            Authentication authentication) {

        User user = authService.getCurrentUser(authentication.getName());
        Event event = eventService.createEvent(title, description, media, user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Event created successfully", event));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Event>> updateEvent(
            @PathVariable Long id,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description,
            @RequestParam(value = "media", required = false) MultipartFile media,
            Authentication authentication) {

        User user = authService.getCurrentUser(authentication.getName());
        Event event = eventService.updateEvent(id, title, description, media, user);
        return ResponseEntity.ok(ApiResponse.ok("Event updated successfully", event));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteEvent(@PathVariable Long id) {
        eventService.deleteEvent(id);
        return ResponseEntity.ok(ApiResponse.ok("Event deleted successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Event>>> getAllEventsAdmin() {
        return ResponseEntity.ok(ApiResponse.ok("Events retrieved successfully", eventService.getAllEvents()));
    }

    @GetMapping("/public")
    public ResponseEntity<ApiResponse<List<Event>>> getPublicEvents() {
        return ResponseEntity.ok(ApiResponse.ok("Events retrieved successfully", eventService.getAllEvents()));
    }

    @GetMapping("/public/{id}")
    public ResponseEntity<ApiResponse<Event>> getPublicEvent(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Event retrieved successfully", eventService.getById(id)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Event>> getEvent(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Event retrieved successfully", eventService.getById(id)));
    }
}
