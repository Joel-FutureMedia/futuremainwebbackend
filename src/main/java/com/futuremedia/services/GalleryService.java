package com.futuremedia.services;

import com.futuremedia.models.*;
import com.futuremedia.repositories.GalleryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
public class GalleryService {

    @Autowired
    private GalleryRepository galleryRepository;

    @Autowired
    private FileStorageService fileStorageService;

    public Gallery uploadSingle(String title, MultipartFile image, User currentUser) {
        if (image == null || image.isEmpty()) {
            throw new BadRequestException("Image is required");
        }

        Gallery gallery = new Gallery();
        gallery.setTitle(title != null ? title.trim() : null);
        gallery.setImagePath(fileStorageService.storeImageOnly(image, "gallery"));
        gallery.setCreatedBy(currentUser);
        return galleryRepository.save(gallery);
    }

    public List<Gallery> uploadMultiple(String title, MultipartFile[] images, User currentUser) {
        if (images == null || images.length == 0) {
            throw new BadRequestException("At least one image is required");
        }

        List<Gallery> saved = new ArrayList<>();
        for (MultipartFile image : images) {
            if (image != null && !image.isEmpty()) {
                Gallery gallery = new Gallery();
                gallery.setTitle(title != null ? title.trim() : null);
                gallery.setImagePath(fileStorageService.storeImageOnly(image, "gallery"));
                gallery.setCreatedBy(currentUser);
                saved.add(galleryRepository.save(gallery));
            }
        }

        if (saved.isEmpty()) {
            throw new BadRequestException("No valid images were uploaded");
        }
        return saved;
    }

    public Gallery updateImage(Long id, String title, MultipartFile image) {
        Gallery gallery = getById(id);

        if (title != null) {
            gallery.setTitle(title.trim());
        }
        if (image != null && !image.isEmpty()) {
            fileStorageService.deleteFile(gallery.getImagePath());
            gallery.setImagePath(fileStorageService.storeImageOnly(image, "gallery"));
        }

        return galleryRepository.save(gallery);
    }

    public void deleteImage(Long id) {
        Gallery gallery = getById(id);
        fileStorageService.deleteFile(gallery.getImagePath());
        galleryRepository.delete(gallery);
    }

    public Gallery getById(Long id) {
        return galleryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Gallery image not found with id: " + id));
    }

    public List<Gallery> getAll() {
        return galleryRepository.findAllByOrderByCreatedDateDesc();
    }
}
