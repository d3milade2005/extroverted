package com.event.service;

import com.event.dto.EventDTO;
import com.event.dto.InteractionDTO;
import com.event.dto.Location;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class EventServiceClient {
    private final RestClient eventClient;

    @Value("${services.event-service.url}")
    private String eventServiceUrl;

    /**
     * Get upcoming approved events
     *
     * @param limit Maximum number of events to fetch
     * @return List of events
     */
    public List<EventDTO> getUpcomingEvents(int limit) {
        try {
            ParameterizedTypeReference<Map<String, Object>> responseType =
                    new ParameterizedTypeReference<>() {};

            Map<String, Object> response = eventClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/events/upcoming")
                            .queryParam("page", 0)
                            .queryParam("size", limit)
                            .build())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(responseType);

            if (response == null) {
                return Collections.emptyList();
            }

            return mapToEventDTOs(response);

        } catch (RestClientException e) {
            log.error("Error fetching upcoming events: {}", e.getMessage());
            return Collections.emptyList();
        }
    }


    /**
     * Get events near a location
     *
     * @param latitude User latitude
     * @param longitude User longitude
     * @param radiusKm Search radius in kilometers
     * @return List of nearby events
     */
    public List<EventDTO> getNearbyEvents(Double latitude, Double longitude, Double radiusKm) {
        try {
            // 1. Define the type: We expect a List of Maps (JSON Objects)
            List<Map<String, Object>> response = eventClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/events/nearby")
                            .queryParam("latitude", latitude)
                            .queryParam("longitude", longitude)
                            .queryParam("radiusKm", radiusKm)
                            .build())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    // 2. Use ParameterizedTypeReference to handle List<Map>
                    .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});

            if (response != null) {
                // 3. Pass the list to your existing mapper
                return mapToEventDTOsList(response);
            }

        } catch (RestClientException e) {
            log.error("Error fetching nearby events: {}", e.getMessage());
        }

        return Collections.emptyList();
    }

    /**
     * Get user interactions (for interaction scoring)
     *
     * @param userId User ID
     * @param token JWT token
     * @return List of user interactions
     */
    public List<InteractionDTO> getUserInteractions(UUID userId, String token) {
        try {
            List<Map<String, Object>> response = eventClient.get()
                    .uri("/api/interactions/user/{userId}", userId)
                    .headers(h -> h.setBearerAuth(token))
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});
            if (response != null) {
                return mapToInteractionDTOs(response);
            }

        } catch (RestClientException e) {
            log.warn("Error fetching user interactions for user {}: {}", userId, e.getMessage());
        }
        return Collections.emptyList();
    }

    /**
     * Get events by category
     *
     * @param categoryId Category ID
     * @param limit Maximum number of events
     * @return List of events in category
     */
    public List<EventDTO> getEventsByCategory(UUID categoryId, int limit) {
        try {
            Map<String, Object> response = eventClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/events/category/{categoryId}")
                            .queryParam("page", 0)
                            .queryParam("size", limit)
                            .build(categoryId))
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            if (response != null){
                return mapToEventDTOs(response);
            }

        } catch (RestClientException e) {
            log.error("Error fetching events by category {}: {}", categoryId, e.getMessage());
        }

        return Collections.emptyList();
    }

    /**
     * Map paginated response to EventDTO list
     */
    @SuppressWarnings("unchecked")
    private List<EventDTO> mapToEventDTOs(Map<String, Object> response) {
        List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");
        if (content == null) {
            return Collections.emptyList();
        }
        return mapToEventDTOsList(content);
    }

    /**
     * Map list of maps to EventDTO list
     */
    @SuppressWarnings("unchecked")
    private List<EventDTO> mapToEventDTOsList(List<?> eventList) {
        List<EventDTO> events = new ArrayList<>();

        for (Object item : eventList) {
            if (item instanceof Map) {
                Map<String, Object> eventData = (Map<String, Object>) item;
                EventDTO event = mapToEventDTO(eventData);
                if (event != null) {
                    events.add(event);
                }
            }
        }

        return events;
    }

    /**
     * Map single event data to EventDTO
     */
    @SuppressWarnings("unchecked")
    private EventDTO mapToEventDTO(Map<String, Object> eventData) {
        try {
            // Extract location
            Location location = null;
            Map<String, Object> locationData = (Map<String, Object>) eventData.get("location");
            if (locationData != null) {
                Double lat = getDoubleValue(locationData.get("latitude"));
                Double lng = getDoubleValue(locationData.get("longitude"));
                if (lat != null && lng != null) {
                    location = Location.builder()
                            .latitude(lat)
                            .longitude(lng)
                            .build();
                }
            }

            return EventDTO.builder()
                    .id(UUID.fromString(eventData.get("id").toString()))
                    .title((String) eventData.get("title"))
                    .description((String) eventData.get("description"))
                    .category((String) eventData.get("category"))
                    .hostId(getUUIDValue(eventData.get("hostId")))
                    .venue((String) eventData.get("venue"))
                    .address((String) eventData.get("address"))
                    .location(location)
                    .startTime(parseDateTime(eventData.get("startTime")))
                    .endTime(parseDateTime(eventData.get("endTime")))
                    .ticketPrice(getBigDecimalValue(eventData.get("ticketPrice")))
                    .ticketLimit(getIntegerValue(eventData.get("ticketLimit")))
                    .ticketsSold(getIntegerValue(eventData.get("ticketsSold")))
                    .imageUrl((String) eventData.get("imageUrl"))
                    .verified((Boolean) eventData.get("verified"))
                    .status((String) eventData.get("status"))
                    .viewCount(getLongValue(eventData.get("viewCount")))
                    .saveCount(getLongValue(eventData.get("saveCount")))
                    .rsvpCount(getLongValue(eventData.get("rsvpCount")))
                    .shareCount(getLongValue(eventData.get("shareCount")))
                    .build();
        } catch (Exception e) {
            log.error("Error mapping event data: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Map interaction list to InteractionDTO list
     */
    @SuppressWarnings("unchecked")
    private List<InteractionDTO> mapToInteractionDTOs(List<?> interactionList) {
        List<InteractionDTO> interactions = new ArrayList<>();

        for (Object item : interactionList) {
            if (item instanceof Map) {
                Map<String, Object> data = (Map<String, Object>) item;
                try {
                    InteractionDTO interaction = InteractionDTO.builder()
                            .id(UUID.fromString(data.get("id").toString()))
                            .userId(getUUIDValue(data.get("userId")))
                            .eventId(getUUIDValue(data.get("eventId")))
                            .type((String) data.get("type"))
                            .category((String) data.get("category"))
                            .createdAt(parseDateTime(data.get("createdAt")))
                            .build();
                    interactions.add(interaction);
                } catch (Exception e) {
                    log.error("Error mapping interaction data: {}", e.getMessage());
                }
            }
        }

        return interactions;
    }

    // Helper methods for type conversion

    private Double getDoubleValue(Object value) {
        if (value == null) return null;
        if (value instanceof Double) return (Double) value;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private UUID getUUIDValue(Object value) {
        if (value == null) return null;
        if (value instanceof UUID) return (UUID) value;
        try {
            return UUID.fromString(value.toString());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private BigDecimal getBigDecimalValue(Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return BigDecimal.valueOf(((Number) value).doubleValue());
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer getIntegerValue(Object value) {
        if (value == null) return null;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long getLongValue(Object value) {
        if (value == null) return 0L;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private LocalDateTime parseDateTime(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDateTime) return (LocalDateTime) value;
        try {
            return LocalDateTime.parse(value.toString(), DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            log.debug("Failed to parse datetime: {}", value);
            return null;
        }
    }
}
