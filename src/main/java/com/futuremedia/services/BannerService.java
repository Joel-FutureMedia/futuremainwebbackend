package com.futuremedia.services;

import com.futuremedia.models.*;
import com.futuremedia.repositories.BannerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class BannerService {

    @Autowired
    private BannerRepository bannerRepository;

    @Autowired
    private FileStorageService fileStorageService;

    public Banner createBanner(String title, String description, MultipartFile image, User currentUser) {
        if (title == null || title.isBlank()) {
            throw new BadRequestException("Title is required");
        }
        if (description == null || description.isBlank()) {
            throw new BadRequestException("Description is required");
        }
        if (image == null || image.isEmpty()) {
            throw new BadRequestException("Banner image is required");
        }

        Banner banner = new Banner();
        banner.setTitle(title.trim());
        banner.setDescription(description.trim());
        banner.setImagePath(fileStorageService.storeImageOnly(image, "banners"));
        banner.setActive(true);
        banner.setCreatedBy(currentUser);
        return bannerRepository.save(banner);
    }

    public Banner updateBanner(Long id, String title, String description, MultipartFile image, Boolean active) {
        Banner banner = getById(id);

        if (title != null && !title.isBlank()) {
            banner.setTitle(title.trim());
        }
        if (description != null && !description.isBlank()) {
            banner.setDescription(description.trim());
        }
        if (image != null && !image.isEmpty()) {
            fileStorageService.deleteFile(banner.getImagePath());
            banner.setImagePath(fileStorageService.storeImageOnly(image, "banners"));
        }
        if (active != null) {
            banner.setActive(active);
        }

        return bannerRepository.save(banner);
    }

    public void deleteBanner(Long id) {
        Banner banner = getById(id);
        fileStorageService.deleteFile(banner.getImagePath());
        bannerRepository.delete(banner);
    }

    public Banner getById(Long id) {
        return bannerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Banner not found with id: " + id));
    }

    public List<Banner> getAll() {
        return bannerRepository.findAllByOrderByCreatedDateDesc();
    }

    public Banner getLatestActive() {
        return bannerRepository.findFirstByActiveTrueOrderByUpdatedDateDesc()
                .orElseThrow(() -> new ResourceNotFoundException("No active banner found"));
    }
}
