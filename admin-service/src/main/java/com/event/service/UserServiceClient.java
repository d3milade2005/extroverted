package com.event.service;

import com.event.dto.UserBatchDTO;
import com.event.dto.UserPreferencesDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.*;
import java.util.stream.Collectors;

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

    public Map<UUID, UserBatchDTO> fetchUsers(Set<UUID> hostIds) {
        try {
            List<UserBatchDTO> users = userClient.post()
                    .uri("/users/batch")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(hostIds)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<UserBatchDTO>>() {});

            // Convert List to Map for fast lookup (O(1) performance)
            // Key = UserID, Value = UserDTO
            return users.stream()
                    .collect(Collectors.toMap(UserBatchDTO::getHostId, user -> user));

        } catch (Exception e) {
            log.error("Failed to fetch user details", e);
            return Collections.emptyMap(); // Return empty map so the main flow doesn't crash
        }
    }
}