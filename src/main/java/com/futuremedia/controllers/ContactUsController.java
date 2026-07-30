package com.futuremedia.controllers;

import com.futuremedia.models.ApiResponse;
import com.futuremedia.models.ContactUs;
import com.futuremedia.services.ContactUsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/contact")
public class ContactUsController {

    @Autowired
    private ContactUsService contactUsService;

    @PostMapping
    public ResponseEntity<ApiResponse<ContactUs>> submit(@RequestBody Map<String, String> body) {
        ContactUs contact = contactUsService.submitAndNotify(
                body.get("fullName"),
                body.get("email"),
                body.get("phone"),
                body.get("subject"),
                body.get("message")
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Contact form submitted successfully. We will get back to you soon.", contact));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ContactUs>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok("Contact submissions retrieved", contactUsService.getAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ContactUs>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Contact submission retrieved", contactUsService.getById(id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> delete(@PathVariable Long id) {
        contactUsService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Contact submission deleted"));
    }
}
