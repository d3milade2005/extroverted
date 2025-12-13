package com.event.controller;

import com.event.dto.*;
import com.event.entity.Event;
import com.event.entity.EventStatus;
import com.event.repository.EventRepository;
import com.event.service.EventService;
import com.event.service.InteractionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.Response;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Slf4j
public class EventController {

    private final EventService eventService;
    private final InteractionService interactionService;

    @PostMapping
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody CreateEventRequest request, Authentication authentication) {
        UUID hostId = getUserId(authentication);
        EventResponse response = eventService.createEvent(hostId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEvent(
            @PathVariable UUID id,
            Authentication authentication) {

        UUID userId = authentication != null ? getUserId(authentication) : null;
        EventResponse response = eventService.getEvent(id, userId);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<EventResponse> updateEvent(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateEventRequest request,
            Authentication authentication) {

        UUID hostId = getUserId(authentication);
        EventResponse response = eventService.updateEvent(id, hostId, request);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(
            @PathVariable UUID id,
            Authentication authentication) {

        UUID hostId = getUserId(authentication);
        eventService.deleteEvent(id, hostId);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/upcoming")
    public ResponseEntity<Page<EventResponse>> getUpcomingEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<EventResponse> events = eventService.getUpcomingEvents(page, size);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<EventResponse>> getEventsNearby(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "10.0") Double radiusKm,
            Authentication authentication) {

        UUID userId = authentication != null ? getUserId(authentication) : null;
        List<EventResponse> events = eventService.findEventsNearby(latitude, longitude, radiusKm, userId);

        return ResponseEntity.ok(events);
    }

    @GetMapping("/search")
    public ResponseEntity<List<EventResponse>> searchEvents(
            @ModelAttribute SearchEventsRequest request,
            Authentication authentication) {

        UUID userId = authentication != null ? getUserId(authentication) : null;
        List<EventResponse> events = eventService.searchEvents(request, userId);

        return ResponseEntity.ok(events);
    }

    @GetMapping("/host/me")
    public ResponseEntity<Page<EventResponse>> getMyEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        UUID hostId = getUserId(authentication);
        Page<EventResponse> events = eventService.getEventsByHost(hostId, page, size);

        return ResponseEntity.ok(events);
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<Page<EventResponse>> getEventsByCategory(
            @PathVariable UUID categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<EventResponse> events = eventService.getEventsByCategory(categoryId, page, size);
        return ResponseEntity.ok(events);
    }

    @PostMapping("/{id}/interactions")
    public ResponseEntity<Void> recordInteraction(
            @PathVariable UUID id,
            @Valid @RequestBody InteractionRequest request,
            Authentication authentication) {

        UUID userId = getUserId(authentication);
        interactionService.recordInteraction(userId, id, request);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{id}/interactions/{type}")
    public ResponseEntity<Void> removeInteraction(
            @PathVariable UUID id,
            @PathVariable String type,
            Authentication authentication) {

        UUID userId = getUserId(authentication);
        interactionService.removeInteraction(
                userId, id, com.event.entity.InteractionType.valueOf(type.toUpperCase()));

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<EventResponse> updateEventStatus(
            @PathVariable UUID id,
            @RequestParam String status) {

        EventResponse response = eventService.updateEventStatus(id, EventStatus.valueOf(status.toUpperCase()));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/getAll")
    public ResponseEntity<Page<EventResponse>> getEvents(@RequestParam(required = false) EventStatus status, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size){
        Page<EventResponse> events = eventService.getEvents(status, page, size);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/getEvent/{id}")
    public ResponseEntity<EventResponse> getEventById(@PathVariable UUID id){
        EventResponse event = eventService.getEventById(id);
        return ResponseEntity.ok(event);
    }


    private UUID getUserId(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        return UUID.fromString(jwt.getSubject());
    }
}