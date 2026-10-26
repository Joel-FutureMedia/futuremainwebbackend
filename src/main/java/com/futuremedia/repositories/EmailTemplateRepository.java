package com.futuremedia.repositories;

import com.futuremedia.models.EmailTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, Long> {

    List<EmailTemplate> findAllByOrderByCreatedDateDesc();

    List<EmailTemplate> findByActiveTrueOrderByCreatedDateDesc();

    boolean existsByNameIgnoreCase(String name);

    java.util.Optional<EmailTemplate> findByNameIgnoreCase(String name);
}
