package com.event.service;

import com.event.dto.EventDTO;
import com.event.dto.InteractionDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class EventServiceClient {

    private final RestClient eventClient;
    private final ObjectMapper objectMapper;

    @Value("${services.event-url:http://event-service:8084}")
    private String eventServiceUrl;

    /**
     * Get upcoming approved events (Handles Page response)
     */
    public List<EventDTO> getUpcomingEvents(int limit) {
        try {
            // 1. Fetch as Raw Map (Safe)
            Map<String, Object> response = eventClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/events/upcoming")
                            .queryParam("page", 0)
                            .queryParam("size", limit)
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            if (response == null || !response.containsKey("content")) {
                return Collections.emptyList();
            }

            // 2. Extract the "content" list (which is a list of LinkedHashMaps)
            List<?> content = (List<?>) response.get("content");

            // 3. Convert Map -> EventDTO using Jackson
            return content.stream()
                    .map(item -> objectMapper.convertValue(item, EventDTO.class))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error fetching upcoming events: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Get events near a location (Handles List response)
     */
    public List<EventDTO> getNearbyEvents(Double latitude, Double longitude, Double radiusKm) {
        try {
            // 1. Fetch as Raw List
            List<Object> response = eventClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/events/nearby")
                            .queryParam("latitude", latitude)
                            .queryParam("longitude", longitude)
                            .queryParam("radiusKm", radiusKm)
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<Object>>() {});

            if (response == null) return Collections.emptyList();

            // 2. Convert Map -> EventDTO
            return response.stream()
                    .map(item -> objectMapper.convertValue(item, EventDTO.class))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error fetching nearby events: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Get user interactions (Handles List response)
     */
    public List<InteractionDTO> getUserInteractions(java.util.UUID userId, String token) {
        try {
            List<Object> response = eventClient.get()
                    .uri("/api/interactions/user/{userId}", userId)
                    .headers(h -> h.setBearerAuth(token))
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<Object>>() {});

            if (response == null) return Collections.emptyList();

            return response.stream()
                    .map(item -> objectMapper.convertValue(item, InteractionDTO.class))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.warn("Error fetching user interactions for user {}: {}", userId, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Get events by category (Handles Page response)
     */
    public List<EventDTO> getEventsByCategory(java.util.UUID categoryId, int limit) {
        try {
            Map<String, Object> response = eventClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/events/category/{categoryId}")
                            .queryParam("page", 0)
                            .queryParam("size", limit)
                            .build(categoryId))
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            if (response == null || !response.containsKey("content")) {
                return Collections.emptyList();
            }

            List<?> content = (List<?>) response.get("content");

            return content.stream()
                    .map(item -> objectMapper.convertValue(item, EventDTO.class))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error fetching events by category {}: {}", categoryId, e.getMessage());
            return Collections.emptyList();
        }
    }
}