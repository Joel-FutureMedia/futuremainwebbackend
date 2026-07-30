package com.futuremedia.services;

import com.futuremedia.models.*;
import com.futuremedia.repositories.NewsRepository;
import com.futuremedia.repositories.SubscriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class NewsService {

    @Autowired
    private NewsRepository newsRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Value("${server.port}")
    private String serverPort;

    public News createNews(String title, String description, MultipartFile media, User currentUser) {
        validateContent(title, description);
        if (media == null || media.isEmpty()) {
            throw new BadRequestException("Image or video is required");
        }

        String mediaPath = fileStorageService.storeImageOrVideo(media, "news");
        String mediaType = fileStorageService.detectMediaType(media);

        News news = new News();
        news.setTitle(title.trim());
        news.setDescription(description.trim());
        news.setMediaPath(mediaPath);
        news.setMediaType(mediaType);
        news.setCreatedBy(currentUser);

        // Super Admin posts are auto-approved
        if (currentUser.getRole() == Role.SUPER_ADMIN) {
            news.setApprovalStatus(ApprovalStatus.APPROVED);
            news.setApprovedBy(currentUser);
            News saved = newsRepository.save(news);
            sendNewsletterToSubscribers(saved);
            return saved;
        }

        news.setApprovalStatus(ApprovalStatus.PENDING);
        return newsRepository.save(news);
    }

    public News updateNews(Long id, String title, String description, MultipartFile media, User currentUser) {
        News news = getById(id);

        boolean isOwner = news.getCreatedBy().getId().equals(currentUser.getId());
        boolean isSuperAdmin = currentUser.getRole() == Role.SUPER_ADMIN;

        if (!isOwner && !isSuperAdmin) {
            throw new UnauthorizedException("You can only edit your own news");
        }

        if (!isSuperAdmin && news.getApprovalStatus() == ApprovalStatus.APPROVED) {
            throw new BadRequestException("Approved news cannot be edited by normal users. Contact Super Admin.");
        }

        if (title != null && !title.isBlank()) {
            news.setTitle(title.trim());
        }
        if (description != null && !description.isBlank()) {
            news.setDescription(description.trim());
        }
        if (media != null && !media.isEmpty()) {
            fileStorageService.deleteFile(news.getMediaPath());
            news.setMediaPath(fileStorageService.storeImageOrVideo(media, "news"));
            news.setMediaType(fileStorageService.detectMediaType(media));
        }

        // If a normal user edits pending/rejected news, keep or reset to PENDING
        if (!isSuperAdmin) {
            news.setApprovalStatus(ApprovalStatus.PENDING);
            news.setApprovedBy(null);
        }

        return newsRepository.save(news);
    }

    public void deleteNews(Long id, User currentUser) {
        News news = getById(id);
        boolean isOwner = news.getCreatedBy().getId().equals(currentUser.getId());
        boolean isSuperAdmin = currentUser.getRole() == Role.SUPER_ADMIN;

        if (!isOwner && !isSuperAdmin) {
            throw new UnauthorizedException("You can only delete your own news");
        }

        if (!isSuperAdmin && news.getApprovalStatus() == ApprovalStatus.APPROVED) {
            throw new BadRequestException("Approved news cannot be deleted by normal users");
        }

        fileStorageService.deleteFile(news.getMediaPath());
        newsRepository.delete(news);
    }

    @Transactional
    public News approveNews(Long id, User superAdmin) {
        if (superAdmin.getRole() != Role.SUPER_ADMIN) {
            throw new UnauthorizedException("Only Super Admin can approve news");
        }
        News news = getById(id);
        news.setApprovalStatus(ApprovalStatus.APPROVED);
        news.setApprovedBy(superAdmin);
        News saved = newsRepository.save(news);
        // Newsletter is async — run after successful save (outside holding a failed TX)
        sendNewsletterToSubscribers(saved);
        return saved;
    }

    public News rejectNews(Long id, User superAdmin) {
        if (superAdmin.getRole() != Role.SUPER_ADMIN) {
            throw new UnauthorizedException("Only Super Admin can reject news");
        }
        News news = getById(id);
        news.setApprovalStatus(ApprovalStatus.REJECTED);
        news.setApprovedBy(superAdmin);
        return newsRepository.save(news);
    }

    public News getById(Long id) {
        return newsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("News not found with id: " + id));
    }

    public List<News> getAllNews() {
        return newsRepository.findAllByOrderByCreatedDateDesc();
    }

    public List<News> getApprovedNews() {
        return newsRepository.findByApprovalStatusOrderByCreatedDateDesc(ApprovalStatus.APPROVED);
    }

    public List<News> getPendingNews() {
        return newsRepository.findByApprovalStatusOrderByCreatedDateDesc(ApprovalStatus.PENDING);
    }

    public List<News> getMyNews(User user) {
        return newsRepository.findByCreatedByOrderByCreatedDateDesc(user);
    }

    private void sendNewsletterToSubscribers(News news) {
        List<Subscription> subscribers = subscriptionRepository.findByActiveTrue();
        String mediaUrl = buildMediaUrl(news.getMediaPath());

        // Detach newsletter payload so @Async work does not reuse the request DB session
        News payload = new News();
        payload.setId(news.getId());
        payload.setTitle(news.getTitle());
        payload.setDescription(news.getDescription());
        payload.setMediaPath(news.getMediaPath());
        payload.setMediaType(news.getMediaType());
        payload.setApprovalStatus(news.getApprovalStatus());
        payload.setCreatedDate(news.getCreatedDate());

        emailService.sendBulkNewsletter(subscribers, payload, mediaUrl);
    }

    private String buildMediaUrl(String mediaPath) {
        if (mediaPath == null) {
            return null;
        }
        if (mediaPath.startsWith("http")) {
            return mediaPath;
        }
        return "http://localhost:" + serverPort + "/" + mediaPath.replace("\\", "/");
    }

    private void validateContent(String title, String description) {
        if (title == null || title.isBlank()) {
            throw new BadRequestException("Title is required");
        }
        if (description == null || description.isBlank()) {
            throw new BadRequestException("Description is required");
        }
    }
}
