package com.futuremedia.controllers;

import com.futuremedia.models.ApiResponse;
import com.futuremedia.models.Brand;
import com.futuremedia.services.BrandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/brands")
public class BrandController {

    @Autowired
    private BrandService brandService;

    @GetMapping("/public")
    public ResponseEntity<ApiResponse<List<Brand>>> getPublic() {
        return ResponseEntity.ok(ApiResponse.ok("Brands retrieved", brandService.getActive()));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Brand>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok("Brands retrieved", brandService.getAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Brand>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Brand retrieved", brandService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Brand>> create(@RequestBody Map<String, Object> body) {
        Brand brand = brandService.create(asString(body.get("name")));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Brand created", brand));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Brand>> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Boolean active = body.get("active") == null ? null : Boolean.valueOf(body.get("active").toString());
        Brand brand = brandService.update(id, asString(body.get("name")), active);
        return ResponseEntity.ok(ApiResponse.ok("Brand updated", brand));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> delete(@PathVariable Long id) {
        brandService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Brand deleted"));
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }
}
