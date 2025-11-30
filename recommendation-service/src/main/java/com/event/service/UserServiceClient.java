package com.event.service;

import com.event.dto.Location;
import com.event.dto.UserPreferencesDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserServiceClient {
    private final RestClient userClient;

    @Value("${services.user-service.url}")
    private String userServiceUrl;

    @Value("${recommendation.cold-start.default-interests}")
    private List<String> defaultInterests;

    /**
     * Get user preferences from User Service
     *
     * @param userId User ID
     * @param token JWT token for authentication
     * @return User preferences or default preferences if user not found
     */
    public UserPreferencesDTO getUserPreferences(UUID userId, String token) {
        try {
            Map<String, Object> response = userClient.get()
                    .uri("/api/users/{id}", userId)
                    .headers(h -> h.setBearerAuth(token))
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
            if (response != null) {
                return mapToUserPreferencesDTO(userId, response);
            }

        } catch (RestClientException e) {
            log.error("Error fetching user preferences for user {}: {}", userId, e.getMessage());
        }
        // Return default preferences on error
        return getDefaultPreferences(userId);
    }

    /**
     * Map User Service response to UserPreferencesDTO
     */
    @SuppressWarnings("unchecked")
    private UserPreferencesDTO mapToUserPreferencesDTO(UUID userId, Map<String, Object> userData) {
        // Extract location
        Location location = null;
        Map<String, Object> locationData = (Map<String, Object>) userData.get("location");
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

        // Extract interests
        List<String> interests = (List<String>) userData.get("interests");
        if (interests == null || interests.isEmpty()) {
            interests = defaultInterests;
        }

        // Extract fashion style
        Map<String, Object> fashionStyle = (Map<String, Object>) userData.get("fashionStyle");

        return UserPreferencesDTO.builder()
                .userId(userId)
                .username((String) userData.get("username"))
                .email((String) userData.get("email"))
                .city((String) userData.get("city"))
                .location(location)
                .interests(interests)
                .fashionStyle(fashionStyle)
                .hasInteractions(false)  // Will be set by checking interactions
                .build();
    }

    /**
     * Get default preferences for cold start users
     */
    private UserPreferencesDTO getDefaultPreferences(UUID userId) {
        log.info("Using default preferences for user {}", userId);

        return UserPreferencesDTO.builder()
                .userId(userId)
                .interests(defaultInterests)
                .hasInteractions(false)
                .build();
    }

    /**
     * Helper to safely extract double values
     */
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
}
