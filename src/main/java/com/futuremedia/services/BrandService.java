package com.futuremedia.services;

import com.futuremedia.models.BadRequestException;
import com.futuremedia.models.Brand;
import com.futuremedia.models.ResourceNotFoundException;
import com.futuremedia.repositories.BrandRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BrandService {

    @Autowired
    private BrandRepository brandRepository;

    public Brand create(String name) {
        String trimmed = requireName(name);
        if (brandRepository.existsByNameIgnoreCase(trimmed)) {
            throw new BadRequestException("Brand already exists: " + trimmed);
        }
        Brand brand = new Brand();
        brand.setName(trimmed);
        brand.setActive(true);
        return brandRepository.save(brand);
    }

    public Brand update(Long id, String name, Boolean active) {
        Brand brand = getById(id);
        if (name != null && !name.isBlank()) {
            String trimmed = name.trim();
            brandRepository.findByNameIgnoreCase(trimmed).ifPresent(existing -> {
                if (!existing.getId().equals(id)) {
                    throw new BadRequestException("Brand already exists: " + trimmed);
                }
            });
            brand.setName(trimmed);
        }
        if (active != null) {
            brand.setActive(active);
        }
        return brandRepository.save(brand);
    }

    public void delete(Long id) {
        Brand brand = getById(id);
        brandRepository.delete(brand);
    }

    public Brand getById(Long id) {
        return brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + id));
    }

    public List<Brand> getAll() {
        return brandRepository.findAllByOrderByNameAsc();
    }

    public List<Brand> getActive() {
        return brandRepository.findByActiveTrueOrderByNameAsc();
    }

    private String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new BadRequestException("Brand name is required");
        }
        return name.trim();
    }
}
