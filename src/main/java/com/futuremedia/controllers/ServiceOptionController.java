package com.futuremedia.controllers;

import com.futuremedia.models.ApiResponse;
import com.futuremedia.models.ServiceOption;
import com.futuremedia.services.ServiceOptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/services")
public class ServiceOptionController {

    @Autowired
    private ServiceOptionService serviceOptionService;

    @GetMapping("/public")
    public ResponseEntity<ApiResponse<List<ServiceOption>>> getPublic() {
        return ResponseEntity.ok(ApiResponse.ok("Services retrieved", serviceOptionService.getActive()));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ServiceOption>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok("Services retrieved", serviceOptionService.getAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ServiceOption>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Service retrieved", serviceOptionService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ServiceOption>> create(@RequestBody Map<String, Object> body) {
        ServiceOption option = serviceOptionService.create(asString(body.get("name")));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Service created", option));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ServiceOption>> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Boolean active = body.get("active") == null ? null : Boolean.valueOf(body.get("active").toString());
        ServiceOption option = serviceOptionService.update(id, asString(body.get("name")), active);
        return ResponseEntity.ok(ApiResponse.ok("Service updated", option));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> delete(@PathVariable Long id) {
        serviceOptionService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Service deleted"));
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }
}
