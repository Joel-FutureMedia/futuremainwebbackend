package com.futuremedia.repositories;

import com.futuremedia.models.GatedDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GatedDocumentRepository extends JpaRepository<GatedDocument, Long> {

    List<GatedDocument> findByActiveTrueOrderByCreatedDateDesc();

    List<GatedDocument> findAllByOrderByCreatedDateDesc();
}
