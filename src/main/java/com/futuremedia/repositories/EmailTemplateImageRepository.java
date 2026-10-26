package com.futuremedia.repositories;

import com.futuremedia.models.EmailTemplateImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailTemplateImageRepository extends JpaRepository<EmailTemplateImage, Long> {
}
