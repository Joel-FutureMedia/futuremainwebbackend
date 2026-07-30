package com.futuremedia.services;

import com.futuremedia.models.*;
import com.futuremedia.repositories.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class EventService {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private FileStorageService fileStorageService;

    public Event createEvent(String title, String description, MultipartFile media, User currentUser) {
        if (title == null || title.isBlank()) {
            throw new BadRequestException("Title is required");
        }
        if (description == null || description.isBlank()) {
            throw new BadRequestException("Description is required");
        }
        if (media == null || media.isEmpty()) {
            throw new BadRequestException("Image or video is required");
        }

        Event event = new Event();
        event.setTitle(title.trim());
        event.setDescription(description.trim());
        event.setMediaPath(fileStorageService.storeImageOrVideo(media, "events"));
        event.setMediaType(fileStorageService.detectMediaType(media));
        event.setCreatedBy(currentUser);
        return eventRepository.save(event);
    }

    public Event updateEvent(Long id, String title, String description, MultipartFile media, User currentUser) {
        Event event = getById(id);

        if (title != null && !title.isBlank()) {
            event.setTitle(title.trim());
        }
        if (description != null && !description.isBlank()) {
            event.setDescription(description.trim());
        }
        if (media != null && !media.isEmpty()) {
            fileStorageService.deleteFile(event.getMediaPath());
            event.setMediaPath(fileStorageService.storeImageOrVideo(media, "events"));
            event.setMediaType(fileStorageService.detectMediaType(media));
        }

        return eventRepository.save(event);
    }

    public void deleteEvent(Long id) {
        Event event = getById(id);
        fileStorageService.deleteFile(event.getMediaPath());
        eventRepository.delete(event);
    }

    public Event getById(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + id));
    }

    public List<Event> getAllEvents() {
        return eventRepository.findAllByOrderByCreatedDateDesc();
    }
}
