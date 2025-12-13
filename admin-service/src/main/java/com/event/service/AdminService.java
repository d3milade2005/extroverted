package com.event.service;

import com.event.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final EventServiceClient eventServiceClient;
    private final UserServiceClient userServiceClient;

    public List<AdminEventsResponse> getEvents(String status, int page, int size) {
        // 1. Get raw events
        List<EventDTO> events = eventServiceClient.getEvents(status, page, size);

        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. Extract Host IDs and Fetch Users
        Set<UUID> hostIds = events.stream()
                .map(EventDTO::getHostId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<UUID, UserBatchDTO> userMap = hostIds.isEmpty()
                ? Collections.emptyMap()
                : userServiceClient.fetchUsers(hostIds);

        return events.stream()
                .map(event -> {
                    // Find the host for this specific event
                    UserBatchDTO hostUser = userMap.get(event.getHostId());

                    // Use the helper method to build the response
                    return mapToAdminResponse(event, hostUser);
                })
                .collect(Collectors.toList());
    }

    public ApproveEventDTO approveEvent(UUID eventId, UUID userId, String adminName) {
        EventDTO event = eventServiceClient.getEventById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found")); // Or custom 404 exception
        event.setStatus("APPROVED");



        AdminUser adminUser = new AdminUser(userId, adminName);

        return ApproveEventDTO.builder()
                .eventId(event.getId())
                .status(event.getStatus())
                .approvedBy(adminUser)
                .approvedAt(LocalDateTime.now())
                .build();
    }

    public RejectEventDTO rejectEvent(UUID eventId, UUID userId, String adminName) {
        EventDTO event = eventServiceClient.getEventById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found")); // Or custom 404 exception
        event.setStatus("CANCELLED");

        AdminUser adminUser = new AdminUser(userId, adminName);

        return RejectEventDTO.builder()
                .eventId(event.getId())
                .status(event.getStatus())
                .rejectedBy(adminUser)
                .rejectedAt(LocalDateTime.now())
                .reason("")
                .allowResubmit(true)
                .build();

    }

    private AdminEventsResponse mapToAdminResponse(EventDTO event, UserBatchDTO hostUser) {
        UserBatchDTO hostDto = null;
        if (hostUser != null) {
            hostDto = new UserBatchDTO(
                    hostUser.getHostId(),
                    hostUser.getHostName(),
                    hostUser.getHostEmail()
            );
        }

        return AdminEventsResponse.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .category(event.getCategoryName())
                .host(hostDto) // Enriched Host Data
                .venue(event.getVenue())
                .address(event.getAddress())
                .location(event.getLocation())
                .startTime(event.getStartTime())
                .endTime(event.getEndTime())
                .createdAt(event.getCreatedAt())
                .ticketPrice(event.getTicketPrice())
                .ticketLimit(event.getTicketLimit())
                .ticketsSold(event.getTicketsSold())
                .imageUrl(event.getImageUrl())
                .verified(event.getVerified())
                .status(event.getStatus())
                .build();
    }
}