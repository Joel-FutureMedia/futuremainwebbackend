package com.futuremedia.services;

import com.futuremedia.models.BadRequestException;
import com.futuremedia.models.ResourceNotFoundException;
import com.futuremedia.models.ServiceOption;
import com.futuremedia.repositories.ServiceOptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ServiceOptionService {

    @Autowired
    private ServiceOptionRepository serviceOptionRepository;

    public ServiceOption create(String name) {
        String trimmed = requireName(name);
        if (serviceOptionRepository.existsByNameIgnoreCase(trimmed)) {
            throw new BadRequestException("Service already exists: " + trimmed);
        }
        ServiceOption option = new ServiceOption();
        option.setName(trimmed);
        option.setActive(true);
        return serviceOptionRepository.save(option);
    }

    public ServiceOption update(Long id, String name, Boolean active) {
        ServiceOption option = getById(id);
        if (name != null && !name.isBlank()) {
            String trimmed = name.trim();
            serviceOptionRepository.findByNameIgnoreCase(trimmed).ifPresent(existing -> {
                if (!existing.getId().equals(id)) {
                    throw new BadRequestException("Service already exists: " + trimmed);
                }
            });
            option.setName(trimmed);
        }
        if (active != null) {
            option.setActive(active);
        }
        return serviceOptionRepository.save(option);
    }

    public void delete(Long id) {
        ServiceOption option = getById(id);
        serviceOptionRepository.delete(option);
    }

    public ServiceOption getById(Long id) {
        return serviceOptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found with id: " + id));
    }

    public List<ServiceOption> getAll() {
        return serviceOptionRepository.findAllByOrderByNameAsc();
    }

    public List<ServiceOption> getActive() {
        return serviceOptionRepository.findByActiveTrueOrderByNameAsc();
    }

    private String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new BadRequestException("Service name is required");
        }
        return name.trim();
    }
}
