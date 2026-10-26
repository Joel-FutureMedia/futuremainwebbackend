package com.futuremedia.controllers;

import com.futuremedia.models.ApiResponse;
import com.futuremedia.models.FreelancerRole;
import com.futuremedia.models.User;
import com.futuremedia.services.AuthService;
import com.futuremedia.services.FreelancerRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/freelancers")
public class FreelancerRoleController {

    @Autowired
    private FreelancerRoleService freelancerRoleService;

    @Autowired
    private AuthService authService;

    @PostMapping
    public ResponseEntity<ApiResponse<FreelancerRole>> create(@RequestBody Map<String, Object> body,
                                                              Authentication authentication) {
        User user = authService.getCurrentUser(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Freelancer role created", freelancerRoleService.create(body, user)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<FreelancerRole>> update(@PathVariable Long id,
                                                              @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(ApiResponse.ok("Freelancer role updated", freelancerRoleService.update(id, body)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> delete(@PathVariable Long id) {
        freelancerRoleService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Freelancer role deleted"));
    }

    @PutMapping("/{id}/publish")
    public ResponseEntity<ApiResponse<FreelancerRole>> publish(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Freelancer role published", freelancerRoleService.publish(id)));
    }

    @PutMapping("/{id}/unpublish")
    public ResponseEntity<ApiResponse<FreelancerRole>> unpublish(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Freelancer role unpublished", freelancerRoleService.unpublish(id)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FreelancerRole>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok("Freelancer roles retrieved", freelancerRoleService.getAll()));
    }

    @GetMapping("/public")
    public ResponseEntity<ApiResponse<List<FreelancerRole>>> getPublic() {
        return ResponseEntity.ok(ApiResponse.ok("Published freelancer roles retrieved", freelancerRoleService.getPublished()));
    }

    @GetMapping("/public/{id}")
    public ResponseEntity<ApiResponse<FreelancerRole>> getPublicById(@PathVariable Long id) {
        FreelancerRole role = freelancerRoleService.getById(id);
        if (!role.isPublished()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.fail("Freelancer role not found"));
        }
        return ResponseEntity.ok(ApiResponse.ok("Freelancer role retrieved", role));
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<ApiResponse<FreelancerRole>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Freelancer role retrieved", freelancerRoleService.getById(id)));
    }
}
