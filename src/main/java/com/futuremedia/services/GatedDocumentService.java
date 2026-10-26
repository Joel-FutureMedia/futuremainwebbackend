package com.futuremedia.services;

import com.futuremedia.models.BadRequestException;
import com.futuremedia.models.GatedDocument;
import com.futuremedia.models.GatedDocumentRequest;
import com.futuremedia.models.ResourceNotFoundException;
import com.futuremedia.models.User;
import com.futuremedia.repositories.GatedDocumentRepository;
import com.futuremedia.repositories.GatedDocumentRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class GatedDocumentService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    @Autowired
    private GatedDocumentRepository gatedDocumentRepository;

    @Autowired
    private GatedDocumentRequestRepository gatedDocumentRequestRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private LeadService leadService;

    @Value("${app.backend.url}")
    private String backendUrl;

    public GatedDocument create(String title, String description, MultipartFile file, User currentUser) {
        if (title == null || title.isBlank()) {
            throw new BadRequestException("Document title is required");
        }
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Document file is required");
        }

        String publicUrl = fileStorageService.storeGatedDocument(file, "gated-documents");
        GatedDocument document = new GatedDocument();
        document.setTitle(title.trim());
        document.setDescription(description != null ? description.trim() : null);
        document.setOriginalFileName(file.getOriginalFilename());
        document.setObjectKey(fileStorageService.extractObjectKey(publicUrl));
        document.setContentType(file.getContentType());
        document.setActive(true);
        document.setCreatedBy(currentUser);
        return gatedDocumentRepository.save(document);
    }

    public GatedDocument update(Long id, String title, String description, MultipartFile file, Boolean active) {
        GatedDocument document = getById(id);
        if (title != null && !title.isBlank()) {
            document.setTitle(title.trim());
        }
        if (description != null) {
            document.setDescription(description.trim());
        }
        if (file != null && !file.isEmpty()) {
            fileStorageService.deleteFile(document.getObjectKey());
            String publicUrl = fileStorageService.storeGatedDocument(file, "gated-documents");
            document.setOriginalFileName(file.getOriginalFilename());
            document.setObjectKey(fileStorageService.extractObjectKey(publicUrl));
            document.setContentType(file.getContentType());
        }
        if (active != null) {
            document.setActive(active);
        }
        return gatedDocumentRepository.save(document);
    }

    @Transactional
    public void delete(Long id) {
        GatedDocument document = getById(id);
        List<GatedDocumentRequest> requests = gatedDocumentRequestRepository.findByDocumentOrderBySubmittedDateDesc(document);
        gatedDocumentRequestRepository.deleteAll(requests);
        fileStorageService.deleteFile(document.getObjectKey());
        gatedDocumentRepository.delete(document);
    }

    public GatedDocument getById(Long id) {
        return gatedDocumentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Gated document not found with id: " + id));
    }

    public List<GatedDocument> getAll() {
        return gatedDocumentRepository.findAllByOrderByCreatedDateDesc();
    }

    public List<GatedDocument> getPublicActive() {
        return gatedDocumentRepository.findByActiveTrueOrderByCreatedDateDesc();
    }

    public GatedDocumentRequest requestDownload(Long documentId, String name, String email,
                                                String telephone, String company) {
        if (name == null || name.isBlank()) {
            throw new BadRequestException("Name is required");
        }
        if (email == null || email.isBlank() || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new BadRequestException("Valid email is required");
        }
        if (telephone == null || telephone.isBlank()) {
            throw new BadRequestException("Telephone is required");
        }
        if (company == null || company.isBlank()) {
            throw new BadRequestException("Company you represent is required");
        }

        GatedDocument document = getById(documentId);
        if (!document.isActive()) {
            throw new BadRequestException("This document is not currently available");
        }

        GatedDocumentRequest request = new GatedDocumentRequest();
        request.setDocument(document);
        request.setName(name.trim());
        request.setEmail(email.trim().toLowerCase());
        request.setTelephone(telephone.trim());
        request.setCompany(company.trim());
        request.setDownloadToken(UUID.randomUUID().toString());
        request.setEmailSent(false);
        GatedDocumentRequest saved = gatedDocumentRequestRepository.save(request);

        leadService.captureQuietly(saved.getName(), saved.getEmail(), saved.getTelephone(),
                saved.getCompany(), "Gate Document Download");

        String downloadUrl = trimSlash(backendUrl) + "/api/gated-documents/download/" + saved.getDownloadToken();
        document.getTitle();
        saved.getEmail();
        emailService.sendGatedDocumentEmail(saved, document, downloadUrl);
        return saved;
    }

    public ResponseEntity<InputStreamResource> downloadByToken(String token) {
        GatedDocumentRequest request = gatedDocumentRequestRepository.findByDownloadToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid or expired download link"));
        return streamDocument(request.getDocument());
    }

    public ResponseEntity<InputStreamResource> downloadById(Long id) {
        return streamDocument(getById(id));
    }

    public List<GatedDocumentRequest> getAllRequests() {
        return gatedDocumentRequestRepository.findAllByOrderBySubmittedDateDesc();
    }

    public List<GatedDocumentRequest> getRequestsByDocument(Long documentId) {
        GatedDocument document = getById(documentId);
        return gatedDocumentRequestRepository.findByDocumentOrderBySubmittedDateDesc(document);
    }

    private ResponseEntity<InputStreamResource> streamDocument(GatedDocument document) {
        String contentType = document.getContentType() != null
                ? document.getContentType()
                : fileStorageService.detectStoredContentType(document.getObjectKey());
        String fileName = clientDownloadFileName(document);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(new InputStreamResource(fileStorageService.openStream(document.getObjectKey())));
    }

    private String clientDownloadFileName(GatedDocument document) {
        String extension = "";
        String original = document.getOriginalFileName();
        if (original != null && original.contains(".")) {
            extension = original.substring(original.lastIndexOf('.'));
        }
        String title = document.getTitle() != null && !document.getTitle().isBlank()
                ? document.getTitle().trim()
                : "document";
        String safeTitle = title.replaceAll("[\\\\/:*?\"<>|]", " ").replace("\"", "").trim();
        if (safeTitle.isBlank()) {
            safeTitle = "document";
        }
        return safeTitle + extension;
    }

    private String trimSlash(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
