package com.futuremedia.services;

import com.futuremedia.models.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final List<String> IMAGE_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "webp");
    private static final List<String> VIDEO_EXTENSIONS = Arrays.asList("mp4", "mov", "avi");
    private static final List<String> DOCUMENT_EXTENSIONS = Arrays.asList("pdf", "doc", "docx");
    private static final List<String> IMAGE_MIME = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/webp"
    );
    private static final List<String> VIDEO_MIME = Arrays.asList(
            "video/mp4", "video/quicktime", "video/x-msvideo", "video/avi"
    );
    private static final List<String> DOCUMENT_MIME = Arrays.asList(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    @Value("${app.upload.dir}")
    private String uploadDir;

    public String storeImageOrVideo(MultipartFile file, String subFolder) {
        validateNotEmpty(file);
        String ext = getExtension(file.getOriginalFilename());
        String contentType = file.getContentType() != null ? file.getContentType().toLowerCase(Locale.ROOT) : "";

        boolean isImage = IMAGE_EXTENSIONS.contains(ext) || IMAGE_MIME.contains(contentType);
        boolean isVideo = VIDEO_EXTENSIONS.contains(ext) || VIDEO_MIME.contains(contentType);

        if (!isImage && !isVideo) {
            throw new BadRequestException(
                    "Invalid file type. Only images (jpg, jpeg, png, webp) or videos (mp4, mov, avi) are allowed.");
        }

        return store(file, subFolder);
    }

    public String storeImageOnly(MultipartFile file, String subFolder) {
        validateNotEmpty(file);
        String ext = getExtension(file.getOriginalFilename());
        String contentType = file.getContentType() != null ? file.getContentType().toLowerCase(Locale.ROOT) : "";

        if (!IMAGE_EXTENSIONS.contains(ext) && !IMAGE_MIME.contains(contentType)) {
            throw new BadRequestException(
                    "Invalid file type. Only images (jpg, jpeg, png, webp) are allowed.");
        }

        return store(file, subFolder);
    }

    public String storeImageOrPdf(MultipartFile file, String subFolder) {
        validateNotEmpty(file);
        String ext = getExtension(file.getOriginalFilename());
        String contentType = file.getContentType() != null ? file.getContentType().toLowerCase(Locale.ROOT) : "";

        boolean isImage = IMAGE_EXTENSIONS.contains(ext) || IMAGE_MIME.contains(contentType);
        boolean isPdf = "pdf".equals(ext) || "application/pdf".equals(contentType);

        if (!isImage && !isPdf) {
            throw new BadRequestException(
                    "Invalid file type. Only images (jpg, jpeg, png, webp) or PDF are allowed for vacancies.");
        }

        return store(file, subFolder);
    }

    public String storeDocumentOnly(MultipartFile file, String subFolder) {
        validateNotEmpty(file);
        String ext = getExtension(file.getOriginalFilename());
        String contentType = file.getContentType() != null ? file.getContentType().toLowerCase(Locale.ROOT) : "";

        if (!DOCUMENT_EXTENSIONS.contains(ext) && !DOCUMENT_MIME.contains(contentType)) {
            throw new BadRequestException(
                    "Invalid file type. Only documents (pdf, doc, docx) are allowed. Images are not accepted.");
        }

        return store(file, subFolder);
    }

    public String detectMediaType(MultipartFile file) {
        String ext = getExtension(file.getOriginalFilename());
        if (IMAGE_EXTENSIONS.contains(ext)) {
            return "IMAGE";
        }
        if (VIDEO_EXTENSIONS.contains(ext)) {
            return "VIDEO";
        }
        if ("pdf".equals(ext)) {
            return "PDF";
        }
        if (DOCUMENT_EXTENSIONS.contains(ext)) {
            return "DOCUMENT";
        }
        String contentType = file.getContentType() != null ? file.getContentType().toLowerCase(Locale.ROOT) : "";
        if (IMAGE_MIME.contains(contentType)) {
            return "IMAGE";
        }
        if (VIDEO_MIME.contains(contentType)) {
            return "VIDEO";
        }
        if ("application/pdf".equals(contentType)) {
            return "PDF";
        }
        return "UNKNOWN";
    }

    public void deleteFile(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return;
        }
        try {
            Path path = Paths.get(uploadDir).resolve(relativePath.replace("uploads/", "").replace("uploads\\", ""));
            if (!Files.exists(path)) {
                path = Paths.get(relativePath);
            }
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // best-effort delete
        }
    }

    private String store(MultipartFile file, String subFolder) {
        try {
            Path folder = Paths.get(uploadDir, subFolder);
            Files.createDirectories(folder);

            String ext = getExtension(file.getOriginalFilename());
            String filename = UUID.randomUUID() + "." + ext;
            Path target = folder.resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            return "uploads/" + subFolder + "/" + filename;
        } catch (IOException e) {
            throw new BadRequestException("Failed to store file: " + e.getMessage());
        }
    }

    private void validateNotEmpty(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required and cannot be empty");
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new BadRequestException("File must have a valid extension");
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
