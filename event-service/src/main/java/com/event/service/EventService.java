package com.event.service;

import com.event.dto.*;
import com.event.entity.Category;
import com.event.entity.Event;
import com.event.entity.EventStatus;
import com.event.entity.InteractionType;
import com.event.repository.CategoryRepository;
import com.event.repository.EventRepository;
import com.event.repository.InteractionRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {

    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final InteractionRepository interactionRepository;
    private final EntityManager entityManager;
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    @Transactional
    public EventResponse createEvent(UUID hostId, CreateEventRequest request) {
        // Validate category exists1
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));
        // Create location point
        Point location = geometryFactory.createPoint(
                new Coordinate(request.getLongitude(), request.getLatitude())
        );
        // Create event
        Event event = Event.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .category(category)
                .hostId(hostId)
                .venue(request.getVenue())
                .address(request.getAddress())
                .location(location)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .imageUrl(request.getImageUrl())
                .ticketPrice(request.getTicketPrice())
                .ticketLimit(request.getTicketLimit())
                .ticketsSold(0)
                .verified(false)
                .status(EventStatus.PENDING)
                .build();

        event = eventRepository.save(event);

        return mapToResponse(event, null, null);
    }

    @Transactional
    public EventResponse updateEvent(UUID eventId, UUID hostId, UpdateEventRequest request) {
        log.info("Updating event: {} by host: {}", eventId, hostId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        // Verify ownership
        if (!event.getHostId().equals(hostId)) {
            throw new RuntimeException("Unauthorized: You are not the host of this event");
        }

        // Update fields if provided
        if (request.getTitle() != null) {
            event.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }
        if (request.getVenue() != null) {
            event.setVenue(request.getVenue());
        }
        if (request.getAddress() != null) {
            event.setAddress(request.getAddress());
        }
        if (request.getLatitude() != null && request.getLongitude() != null) {
            Point location = geometryFactory.createPoint(
                    new Coordinate(request.getLongitude(), request.getLatitude())
            );
            event.setLocation(location);
        }
        if (request.getStartTime() != null) {
            event.setStartTime(request.getStartTime());
        }
        if (request.getEndTime() != null) {
            event.setEndTime(request.getEndTime());
        }
        if (request.getImageUrl() != null) {
            event.setImageUrl(request.getImageUrl());
        }
        if (request.getTicketPrice() != null) {
            event.setTicketPrice(request.getTicketPrice());
        }
        if (request.getTicketLimit() != null) {
            event.setTicketLimit(request.getTicketLimit());
        }

        event = eventRepository.save(event);
        log.info("Event updated successfully: {}", eventId);

        return mapToResponse(event, null, null);
    }

    @Transactional(readOnly = true)
    public EventResponse getEvent(UUID eventId, UUID userId) {
        log.info("Fetching event: {}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        Boolean isSaved = null;
        Boolean hasRsvp = null;

        if (userId != null) {
            isSaved = interactionRepository.existsByUserIdAndEventIdAndType(
                    userId, eventId, InteractionType.SAVE);
            hasRsvp = interactionRepository.existsByUserIdAndEventIdAndType(
                    userId, eventId, InteractionType.RSVP);
        }

        return mapToResponse(event, isSaved, hasRsvp);
    }

    @Transactional(readOnly = true)
    public Page<EventResponse> getUpcomingEvents(int page, int size) {
        log.info("Fetching upcoming events - page: {}, size: {}", page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("startTime").ascending());
        Page<Event> events = eventRepository.findUpcomingEvents(
                LocalDateTime.now(), EventStatus.APPROVED, pageable);

        return events.map(event -> mapToResponse(event, null, null));
    }

    @Transactional(readOnly = true)
    public Page<EventResponse> getEventsByHost(UUID hostId, int page, int size) {
        log.info("Fetching events for host: {}", hostId);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Event> events = eventRepository.findByHostId(hostId, pageable);

        return events.map(event -> mapToResponse(event, null, null));
    }

    @Transactional(readOnly = true)
    public Page<EventResponse> getEventsByCategory(UUID categoryId, int page, int size) {
        log.info("Fetching events for category: {}", categoryId);

        Pageable pageable = PageRequest.of(page, size, Sort.by("startTime").ascending());
        Page<Event> events = eventRepository.findByCategoryId(categoryId, pageable);

        return events.map(event -> mapToResponse(event, null, null));
    }

    @Transactional(readOnly = true)
    public List<EventResponse> findEventsNearby(Double latitude, Double longitude, Double radiusKm, UUID userId) {
        log.info("Finding events nearby: lat={}, lng={}, radius={}km", latitude, longitude, radiusKm);

        Double radiusMeters = radiusKm * 1000;

        // 1. Native Query: Get only ID and Distance
        // row[0] = String ID, row[1] = Double Distance
        List<Object[]> results = eventRepository.findEventsNearby(
                latitude, longitude, radiusMeters, LocalDateTime.now());

        if (results.isEmpty()) return Collections.emptyList();

        // 2. Batch Processing to prevent N+1 Problem
        return processEventResults(results, userId);
    }

    @Transactional(readOnly = true)
    public List<EventResponse> searchEvents(SearchEventsRequest request, UUID userId) {
        log.info("Searching events with filters: {}", request);

        Double radiusMeters = request.getRadiusKm() != null ? request.getRadiusKm() * 1000 : null;

        // 1. Native Query
        // IMPORTANT: Ensure EventRepository.searchEvents returns SELECT CAST(e.id as text), distance...
        List<Object[]> results = eventRepository.searchEvents(
                request.getCategoryId() != null ? request.getCategoryId().toString() : null,
                request.getStatus(),
                request.getVerifiedOnly(),
                request.getFreeOnly(),
                request.getAvailableTicketsOnly(),
                request.getStartDate(),
                request.getEndDate(),
                request.getMinPrice() != null ? request.getMinPrice().doubleValue() : null,
                request.getMaxPrice() != null ? request.getMaxPrice().doubleValue() : null,
                request.getKeyword(),
                request.getLatitude(),
                request.getLongitude(),
                radiusMeters,
                request.getSortBy(),
                request.getSortDirection()
        );

        if (results.isEmpty()) return Collections.emptyList();

        // 2. Batch Processing
        return processEventResults(results, userId);
    }

    /**
     * Helper method to fetch Entities in batch and map them to results
     * Prevents N+1 Query Problem
     */
    private List<EventResponse> processEventResults(List<Object[]> results, UUID userId) {
        // A. Extract all IDs
        List<UUID> eventIds = results.stream()
                .map(row -> UUID.fromString((String) row[0]))
                .collect(Collectors.toList());

        // B. Fetch all Entities in ONE query (SELECT * FROM events WHERE id IN (...))
        Map<UUID, Event> eventMap = eventRepository.findAllById(eventIds).stream()
                .collect(Collectors.toMap(Event::getId, Function.identity()));

        // C. Fetch all User Interactions in ONE query (Optimization)
        Set<UUID> savedEvents = new HashSet<>();
        Set<UUID> rsvpEvents = new HashSet<>();

        if (userId != null && !eventIds.isEmpty()) {
            // Ideally, add a method in InteractionRepository to fetch list of interaction types for these event IDs
            // For now, we can leave the loop for interactions or implement:
            // interactionRepository.findByUserIdAndEventIdIn(userId, eventIds)...
            // But let's stick to the simple check inside mapToResponse logic for now,
            // OR do the proper interaction batch fetch if you have the repo method.
        }

        // D. Reconstruct the list preserving the original order (Sorting)
        return results.stream()
                .map(row -> {
                    UUID eventId = UUID.fromString((String) row[0]);
                    Event event = eventMap.get(eventId);

                    if (event == null) return null; // Should not happen

                    Double distanceKm = row[1] != null ? ((Number) row[1]).doubleValue() : null;

                    // Interaction Checks (Still one-by-one here unless we add a batch interaction query)
                    // This is acceptable for page sizes of 20-50.
                    Boolean isSaved = userId != null ?
                            interactionRepository.existsByUserIdAndEventIdAndType(userId, eventId, InteractionType.SAVE) : null;
                    Boolean hasRsvp = userId != null ?
                            interactionRepository.existsByUserIdAndEventIdAndType(userId, eventId, InteractionType.RSVP) : null;

                    EventResponse response = mapToResponse(event, isSaved, hasRsvp);
                    response.setDistanceKm(distanceKm);
                    return response;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteEvent(UUID eventId, UUID hostId) {
        log.info("Deleting event: {} by host: {}", eventId, hostId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        // Verify ownership
        if (!event.getHostId().equals(hostId)) {
            throw new RuntimeException("Unauthorized: You are not the host of this event");
        }

        eventRepository.delete(event);
        log.info("Event deleted successfully: {}", eventId);
    }

    @Transactional
    public EventResponse updateEventStatus(UUID eventId, EventStatus status) {
        log.info("Updating event status: {} to {}", eventId, status);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        event.setStatus(status);
        event = eventRepository.save(event);

        log.info("Event status updated successfully: {}", eventId);
        return mapToResponse(event, null, null);
    }

    @Transactional(readOnly = true)
    public Page<EventResponse> getEvents(EventStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("startTime").ascending());

        Page<Event> events;

        if (status == null) {
            events = eventRepository.findAll(pageable);
        }
        else {
            events = eventRepository.findByStatus(status, pageable);
        }

        return events.map(event -> mapToResponse(event, null, null));
    }

    @Transactional(readOnly = true)
    public EventResponse getEventById(UUID eventId) {
        Event event = eventRepository.findById(eventId).orElseThrow(() -> new RuntimeException("Event not found"));
        return mapToResponse(event, null, null);
    }



    private EventResponse mapToResponse(Event event, Boolean isSaved, Boolean hasRsvp) {
        return EventResponse.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .category(mapCategoryToResponse(event.getCategory()))
                .hostId(event.getHostId())
                .venue(event.getVenue())
                .address(event.getAddress())
                .location(EventResponse.LocationResponse.builder()
                        .latitude(event.getLocation().getY())
                        .longitude(event.getLocation().getX())
                        .build())
                .startTime(event.getStartTime())
                .endTime(event.getEndTime())
                .imageUrl(event.getImageUrl())
                .ticketPrice(event.getTicketPrice())
                .ticketLimit(event.getTicketLimit())
                .ticketsSold(event.getTicketsSold())
                .remainingTickets(event.getRemainingTickets())
                .hasAvailableTickets(event.hasAvailableTickets())
                .verified(event.getVerified())
                .status(event.getStatus())
                .isSaved(isSaved)
                .hasRsvp(hasRsvp)
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .build();
    }

    private CategoryResponse mapCategoryToResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .icon(category.getIcon())
                .build();
    }
}