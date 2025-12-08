package com.event.service;

import com.event.dto.UserPreferencesDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
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
    private final ObjectMapper objectMapper;

    @Value("${services.user-service.url:http://user-service:8083}")
    private String userServiceUrl;

    @Value("${recommendation.cold-start.default-interests}")
    private List<String> defaultInterests;

    public UserPreferencesDTO getUserPreferences(UUID userId, String token) {
        try {
            // 1. Fetch as Raw Map
            Map<String, Object> response = userClient.get()
                    .uri("/api/users/{id}", userId)
                    .headers(h -> h.setBearerAuth(token))
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            if (response != null) {
                // 2. Convert to DTO using Jackson
                UserPreferencesDTO dto = objectMapper.convertValue(response, UserPreferencesDTO.class);

                // 3. Fix ID Mapping (Database has 'keycloakId', DTO has 'userId')
                // If Jackson didn't populate userId, grab keycloakId from the map
                if (dto.getUserId() == null && response.containsKey("id")) {
                    dto.setUserId(UUID.fromString((String) response.get("id")));
                }

                // 4. Apply Cold Start Logic (Default Interests)
                if (dto.getInterests() == null || dto.getInterests().isEmpty()) {
                    log.info("User {} has no interests, applying defaults", userId);
                    dto.setInterests(defaultInterests);
                }

                return dto;
            }

        } catch (RestClientException e) {
            log.error("Error fetching user preferences for user {}: {}", userId, e.getMessage());
        } catch (Exception e) {
            log.error("Error mapping user preferences: {}", e.getMessage());
        }

        // Return default preferences on error
        return getDefaultPreferences(userId);
    }

    private UserPreferencesDTO getDefaultPreferences(UUID userId) {
        log.info("Using default preferences for user {}", userId);
        return UserPreferencesDTO.builder()
                .userId(userId)
                .interests(defaultInterests)
                .hasInteractions(false)
                .build();
    }
}