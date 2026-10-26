package com.futuremedia.repositories;

import com.futuremedia.models.GatedDocument;
import com.futuremedia.models.GatedDocumentRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GatedDocumentRequestRepository extends JpaRepository<GatedDocumentRequest, Long> {

    Optional<GatedDocumentRequest> findByDownloadToken(String downloadToken);

    List<GatedDocumentRequest> findAllByOrderBySubmittedDateDesc();

    List<GatedDocumentRequest> findByDocumentOrderBySubmittedDateDesc(GatedDocument document);
}
