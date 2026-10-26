package com.futuremedia.services;

import com.futuremedia.models.BadRequestException;
import com.futuremedia.models.ResourceNotFoundException;
import com.futuremedia.models.SalesOption;
import com.futuremedia.repositories.SalesOptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SalesOptionService {

    @Autowired
    private SalesOptionRepository salesOptionRepository;

    public SalesOption create(String name) {
        String trimmed = requireName(name);
        if (salesOptionRepository.existsByNameIgnoreCase(trimmed)) {
            throw new BadRequestException("Sales option already exists: " + trimmed);
        }
        SalesOption option = new SalesOption();
        option.setName(trimmed);
        option.setActive(true);
        return salesOptionRepository.save(option);
    }

    public SalesOption update(Long id, String name, Boolean active) {
        SalesOption option = getById(id);
        if (name != null && !name.isBlank()) {
            String trimmed = name.trim();
            salesOptionRepository.findByNameIgnoreCase(trimmed).ifPresent(existing -> {
                if (!existing.getId().equals(id)) {
                    throw new BadRequestException("Sales option already exists: " + trimmed);
                }
            });
            option.setName(trimmed);
        }
        if (active != null) {
            option.setActive(active);
        }
        return salesOptionRepository.save(option);
    }

    public void delete(Long id) {
        SalesOption option = getById(id);
        salesOptionRepository.delete(option);
    }

    public SalesOption getById(Long id) {
        return salesOptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sales option not found with id: " + id));
    }

    public List<SalesOption> getAll() {
        return salesOptionRepository.findAllByOrderByNameAsc();
    }

    public List<SalesOption> getActive() {
        return salesOptionRepository.findByActiveTrueOrderByNameAsc();
    }

    private String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new BadRequestException("Sales option name is required");
        }
        return name.trim();
    }
}
