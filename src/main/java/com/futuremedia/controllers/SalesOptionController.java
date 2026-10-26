package com.futuremedia.controllers;

import com.futuremedia.models.ApiResponse;
import com.futuremedia.models.SalesOption;
import com.futuremedia.services.SalesOptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sales")
public class SalesOptionController {

    @Autowired
    private SalesOptionService salesOptionService;

    @GetMapping("/public")
    public ResponseEntity<ApiResponse<List<SalesOption>>> getPublic() {
        return ResponseEntity.ok(ApiResponse.ok("Sales options retrieved", salesOptionService.getActive()));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SalesOption>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok("Sales options retrieved", salesOptionService.getAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SalesOption>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Sales option retrieved", salesOptionService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SalesOption>> create(@RequestBody Map<String, Object> body) {
        SalesOption option = salesOptionService.create(asString(body.get("name")));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Sales option created", option));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SalesOption>> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Boolean active = body.get("active") == null ? null : Boolean.valueOf(body.get("active").toString());
        SalesOption option = salesOptionService.update(id, asString(body.get("name")), active);
        return ResponseEntity.ok(ApiResponse.ok("Sales option updated", option));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> delete(@PathVariable Long id) {
        salesOptionService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Sales option deleted"));
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }
}
