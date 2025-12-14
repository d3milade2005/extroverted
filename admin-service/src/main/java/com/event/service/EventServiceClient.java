package com.event.service;

import com.event.dto.EventDTO;
import com.event.dto.InteractionDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class EventServiceClient {

    private final RestClient eventClient;
    private final ObjectMapper objectMapper;

    @Value("${services.event-url:http://event-service:8084}")
    private String eventServiceUrl;


    public List<EventDTO> getEvents(String status, int page, int size) {
        try {
            // 1. Fetch as Raw Map
            Map<String, Object> response = eventClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/events/getAll") // Ensure this matches your Controller path
                            // Only adds "status" parameter if it is NOT null
                            .queryParamIfPresent("status", Optional.ofNullable(status))
                            .queryParam("page", page)
                            .queryParam("size", size)
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            if (response == null || !response.containsKey("content")) {
                return Collections.emptyList();
            }

            // 2. Extract content
            List<?> content = (List<?>) response.get("content");

            // 3. Convert to DTOs
            return content.stream()
                    .map(item -> objectMapper.convertValue(item, EventDTO.class))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error fetching events with status {}: {}", status, e.getMessage());
            return Collections.emptyList();
        }
    }

    public Optional<EventDTO> getEventById(UUID eventId) {
        try {
            EventDTO event = eventClient.get()
                    .uri("/api/events/getEvent/{id}", eventId)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                    })
                    .body(EventDTO.class);

            return Optional.ofNullable(event);

        } catch (Exception e) {
            log.error("Failed to fetch event {}: {}", eventId, e.getMessage());
            return Optional.empty();
        }
    }

    public void updateEventStatus(UUID eventId, String status) {
        try {
            eventClient.patch()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/events/{id}/status") // Match your Controller's path
                            .queryParam("status", status)    // Add ?status=APPROVED
                            .build(eventId))                 // Replace {id} with eventId
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                        throw new RuntimeException("Invalid status update request");
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                        throw new RuntimeException("Event Service is down");
                    })
                    .toBodilessEntity(); // We don't need the return object, just success (200 OK)

        } catch (Exception e) {
            log.error("Failed to update status for event {}: {}", eventId, e.getMessage());
            throw new RuntimeException("Could not communicate with Event Service");
        }
    }
}