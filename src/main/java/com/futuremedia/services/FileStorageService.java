package com.futuremedia.services;

import com.futuremedia.models.BadRequestException;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Stores all files (images, videos, CVs, CSVs, documents) in MinIO S3.
 * PostgreSQL only keeps the public URL / object key — never file bytes.
 */
@Service
public class FileStorageService {

    private static final List<String> IMAGE_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "webp");
    private static final List<String> VIDEO_EXTENSIONS = Arrays.asList("mp4", "mov", "avi");
    private static final List<String> DOCUMENT_EXTENSIONS = Arrays.asList("pdf", "doc", "docx", "csv");
    private static final List<String> IMAGE_MIME = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/webp"
    );
    private static final List<String> VIDEO_MIME = Arrays.asList(
            "video/mp4", "video/quicktime", "video/x-msvideo", "video/avi"
    );
    private static final List<String> DOCUMENT_MIME = Arrays.asList(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/csv",
            "application/csv",
            "application/vnd.ms-excel"
    );

    @Autowired
    private MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucket;

    @Value("${minio.public-url}")
    private String publicUrl;

    public String storeImageOrVideo(MultipartFile file, String subFolder) {
        validateNotEmpty(file);
        String ext = getExtension(file.getOriginalFilename());
        String contentType = mime(file);

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
        String contentType = mime(file);

        if (!IMAGE_EXTENSIONS.contains(ext) && !IMAGE_MIME.contains(contentType)) {
            throw new BadRequestException(
                    "Invalid file type. Only images (jpg, jpeg, png, webp) are allowed.");
        }

        return store(file, subFolder);
    }

    public String storeImageOrPdf(MultipartFile file, String subFolder) {
        validateNotEmpty(file);
        String ext = getExtension(file.getOriginalFilename());
        String contentType = mime(file);

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
        String contentType = mime(file);

        if (!DOCUMENT_EXTENSIONS.contains(ext) && !DOCUMENT_MIME.contains(contentType)) {
            throw new BadRequestException(
                    "Invalid file type. Only documents (pdf, doc, docx, csv) are allowed. Images are not accepted.");
        }

        return store(file, subFolder);
    }

    public String storeGatedDocument(MultipartFile file, String subFolder) {
        return storeDocumentOnly(file, subFolder);
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
        if ("csv".equals(ext)) {
            return "CSV";
        }
        if (DOCUMENT_EXTENSIONS.contains(ext)) {
            return "DOCUMENT";
        }
        String contentType = mime(file);
        if (IMAGE_MIME.contains(contentType)) {
            return "IMAGE";
        }
        if (VIDEO_MIME.contains(contentType)) {
            return "VIDEO";
        }
        if ("application/pdf".equals(contentType)) {
            return "PDF";
        }
        if (DOCUMENT_MIME.contains(contentType)) {
            return "DOCUMENT";
        }
        return "UNKNOWN";
    }

    public void deleteFile(String storedPathOrUrl) {
        String objectKey = extractObjectKey(storedPathOrUrl);
        if (objectKey == null || objectKey.isBlank()) {
            return;
        }
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build());
        } catch (Exception ignored) {
            // best-effort delete
        }
    }

    public InputStream openStream(String storedPathOrUrl) {
        String objectKey = extractObjectKey(storedPathOrUrl);
        if (objectKey == null || objectKey.isBlank()) {
            throw new BadRequestException("File path is missing");
        }
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build());
        } catch (Exception e) {
            throw new BadRequestException("Failed to read file from storage: " + e.getMessage());
        }
    }

    public String detectStoredContentType(String storedPathOrUrl) {
        String objectKey = extractObjectKey(storedPathOrUrl);
        if (objectKey == null || objectKey.isBlank()) {
            return "application/octet-stream";
        }
        try {
            StatObjectResponse stat = minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build());
            if (stat.contentType() != null && !stat.contentType().isBlank()) {
                return stat.contentType();
            }
        } catch (Exception ignored) {
            // fall through
        }
        return "application/octet-stream";
    }

    public String extractObjectKey(String storedPathOrUrl) {
        if (storedPathOrUrl == null || storedPathOrUrl.isBlank()) {
            return null;
        }
        String value = storedPathOrUrl.trim();
        String base = trimSlash(publicUrl);
        if (value.startsWith(base + "/")) {
            return value.substring(base.length() + 1);
        }
        if (value.startsWith("http://") || value.startsWith("https://")) {
            int bucketIdx = value.indexOf("/" + bucket + "/");
            if (bucketIdx >= 0) {
                return value.substring(bucketIdx + bucket.length() + 2);
            }
        }
        if (value.startsWith("uploads/")) {
            return value.substring("uploads/".length());
        }
        return value.replace("\\", "/");
    }

    public String toPublicUrl(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }
        if (objectKey.startsWith("http://") || objectKey.startsWith("https://")) {
            return objectKey;
        }
        return trimSlash(publicUrl) + "/" + objectKey.replace("\\", "/");
    }

    private String store(MultipartFile file, String subFolder) {
        try {
            String ext = getExtension(file.getOriginalFilename());
            String filename = UUID.randomUUID() + "." + ext;
            String objectKey = trimSlash(subFolder) + "/" + filename;
            String contentType = file.getContentType() != null && !file.getContentType().isBlank()
                    ? file.getContentType()
                    : "application/octet-stream";

            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(contentType)
                    .build());

            return toPublicUrl(objectKey);
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Failed to store file in MinIO: " + e.getMessage());
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

    private String mime(MultipartFile file) {
        return file.getContentType() != null ? file.getContentType().toLowerCase(Locale.ROOT) : "";
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
